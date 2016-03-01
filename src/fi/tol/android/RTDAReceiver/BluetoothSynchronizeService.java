/**
 *  Copyright 2015   PIKESTA, FINLAND
 *
 *
 * 	This file is part of PalveluApu tool.
 * 	PalveluApu is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License version 2 (GPLv2) as published by
 *  the Free Software Foundation.
 * 	PalveluApu is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License version 2 for
 *  more details.
 * 	You should have received a copy of the GNU General Public License version 2
 *  along with RTDAReceiver.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html/>.
 */

/**
 * Class BluetoothSynchronizeService
 *
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 *
 */
package fi.tol.android.RTDAReceiver;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BluetoothSynchronizeService {   
    private Context appContext;

    /** Name for the SDP record when creating server socket */
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    /** Unique UUID for this application */
    private static final UUID UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    /** Member fields */
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    /** Constants that indicate the current connection state */
    /** We're doing nothing */
    public static final int STATE_NONE = 0;
    /** Now listening for incoming connections */
    public static final int STATE_LISTEN = 1;
    /** now initiating an outgoing connection */
    public static final int STATE_CONNECTING = 2;
    /** now connected to a remote device */
    public static final int STATE_CONNECTED = 3;
    
    private boolean isModuleEnded = false;
    private boolean isServer = false;
    private String username;
    private boolean isTransferFinished = false;
    
    /** File read and write attributes */
    private static FileOutputStream fileOut;
	private static OutputStreamWriter outWriter;
	private static BufferedWriter bufferWriter;
	
	/** How many files to receive */
	private int receiveFileNum = 0;
	/** How many files have been received */
	private int receivedNum = 0;
	
	/** How many files to send */
	private int sendFileNum = 0;
	/** How many files have been sent */
	private int sentNum = 0;

    /** Constructor. Prepares a new BluetoothSynchronize session.
     * @param handler  A Handler to send messages back to the UI Activity */
    public BluetoothSynchronizeService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /** Set the current state of the synchronize connection */
    private synchronized void setState(int state) {
        mState = state;
    }

    /** Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /** Start the synchronize service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. */
    public synchronized void start() {

        /** Cancel any thread attempting to make a connection */
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        /** Cancel any thread currently running a connection */
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

         setState(STATE_LISTEN);

        /** Start the thread to listen on a BluetoothServerSocket */
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /** Start the ConnectThread to initiate a connection to a remote device */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        /** Cancel any thread attempting to make a connection */
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        /** Cancel any thread currently running a connection */
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        /** Start the thread to connect with the given device */
        mConnectThread = new ConnectThread(device,secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /** Start the ConnectedThread to begin managing a Bluetooth connection */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        /** Cancel the thread that completed the connection */
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        /** Cancel any thread currently running a connection */
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        /** Cancel the accept thread because we only want to connect to one device */
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        /** Start the thread to manage the connection and perform transmissions */
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        /** Send the name of the connected device back to the UI Activity */
        Message msg = mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSynchronize.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /** Stop all threads */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /** Write to the ConnectedThread in an unsynchronized manner */
    public void write(byte[] out) {
        /** Create temporary object */
        ConnectedThread r;
        /** Synchronize a copy of the ConnectedThread */
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        /** Perform the write unsynchronized */
        r.write(out);
    }

    /** Indicate that the connection attempt failed and notify the UI Activity. */
    private void connectionFailed() {
        /** Send a failure message back to the Activity */
        Message msg = mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSynchronize.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        /** Start the service over to restart listening mode */
        BluetoothSynchronizeService.this.start();
    }

    /** Indicate that the connection was lost and notify the UI Activity. */
    private void connectionLost() {
        /** Send a failure message back to the Activity */
    	String connectionLostMsg = "";
    	if(isTransferFinished == false)
    	{
    		connectionLostMsg = appContext.getString(R.string.bt_connection_lost);
    	}
    	else if(isTransferFinished == true)
    	{
    		connectionLostMsg = appContext.getString(R.string.stop_connection);
    	}
    	
    	Message msg = mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSynchronize.TOAST, connectionLostMsg);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        initiateForNewService();
    }
    
    /** After transferring finished, initiate both devices */
    public void initiateForNewService()
    {
    	if(isModuleEnded == false)
        {
        	/** Send a initiate UI message back to the Activity */
            Message initUiMsg = mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_INITIATE_UI);
            mHandler.sendMessage(initUiMsg);
            
        	/** Start the service over to restart listening mode */
            BluetoothSynchronizeService.this.start();
        }
    }
    
    /** BluetoothSynchronize Activity inform the end of the applicatino */
    public void setIsModuleEnded(boolean isEnded)
    {
    	isModuleEnded = isEnded;
    }
    
    /** BluetoothSynchronize Activity inform whether the device is currently acting as server or not */
    public void setIsServer(boolean server)
    {
    	isServer = server;
    }
    /** BluetoothSynchronize Activity inform the currently login user name */
    public void setLoginUserName(String name)
    {
    	username = name;
    }
    /** BluetoothSynchronize Activity inform the transferring is finished */
    public void setIsTransferFinished(boolean isFinished)
    {
    	isTransferFinished = isFinished;
    }
    /** BluetoothSynchronize Activity inform the application context */
    public void setContext(Context context)
    {
    	appContext = context;
    }
    /** BluetoothSynchronize Activity inform the number of files to send */
    public void setSendFileNum(int fileNum)
    {
    	sendFileNum = fileNum;
    	sentNum = 0;
    }

    /** This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until canceled). */
    private class AcceptThread extends Thread {
        /** The local server socket */
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            /** Create a new listening server socket */
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, UUID_INSECURE);
                }
            } catch (IOException e) {
            	e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            /** Listen to the server socket if we're not connected */
            while (mState != STATE_CONNECTED) {
                try {
                    /** This is a blocking call and will only return on a successful connection or an exception */
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                	e.printStackTrace();
                    break;
                }

                /** If a connection was accepted */
                if (socket != null) {
                    synchronized (BluetoothSynchronizeService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            /** Situation normal. Start the connected thread. */
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            /** Either not ready or already connected. Terminate new socket. */
                            try {
                                socket.close();
                            } catch (IOException e) {
                            	e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
    }


    /** This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails. */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            /** Get a BluetoothSocket for a connection with the given BluetoothDevice */
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            UUID_INSECURE);
                }
            } catch (IOException e) {
            	e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread" + mSocketType);

            /** Always cancel discovery because it will slow down a connection */
            mAdapter.cancelDiscovery();

            /** Make a connection to the BluetoothSocket */
            try {
                /** This is a blocking call and will only return on a successful connection or an exception */
                mmSocket.connect();
            } catch (IOException e) {
                /** Close the socket */
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                	e.printStackTrace();
                }
                connectionFailed();
                return;
            }

            /** Reset the ConnectThread because we're done */
            synchronized (BluetoothSynchronizeService.this) {
                mConnectThread = null;
            }

            /** Start the connected thread */
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
    }

    /** This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions. */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private String filePath = "";
        private boolean isBeginMark = false;
    	private boolean isEndMark = true;
    	private boolean fileAppend = false;
    

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            /** Get the BluetoothSocket input and output streams */
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            	e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            /** Keep listening to the InputStream while connected */
            while (true) {
                try {
                    /** Read from the InputStream */
                    bytes = mmInStream.read(buffer);
                    
                    /** If the device is server side, it will receive data from client
                     *  and write data into corresponding files */
                    if(isServer == true)
                    {
                    	writeDataFromClientToFile(buffer,bytes);
                    }
                    else
                    {
                    	/** Send the obtained bytes to the UI Activity */
                        mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                    
                } catch (IOException e) {
                	e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }

        /** Write to the connected OutStream. */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                /** Share the sent message back to the UI Activity */
                if(isServer == true)
                {
                	receiveFileNum = 0;
                	receivedNum = 0;
                	mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                }
                else if(isServer == false)
                {
                	if(sendFileNum != 0)
                	{
                    	sentNum ++;
            			int finished;
            			if(sentNum == sendFileNum)
            			{
            				finished = 100;
            			}
            			else
            			{
            				finished = (sentNum *100)/sendFileNum; 
            			}
            
            			mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_PROGRESS_BAR, finished, -1, null).sendToTarget();
                    	
                	}
                }
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
        
        /** Write the data received from the client side to corresponding files */
        public void writeDataFromClientToFile(byte[] buffer, int bytes)
        {
        	String readMessage = new String(buffer, 0, bytes);
            
        	String[] tempArr = readMessage.split("@");
        	String fileData = "";
        	File file = new File(filePath);
        	
        	
        	for(int i = 0; i < tempArr.length; i++)
        	{
        		
        		if(i == (tempArr.length - 1) && tempArr[i].equals(BluetoothSynchronize.FILE_END_MARK) == false)
        		{
        			fileData += tempArr[i];
        			try{
        				fileOut = new FileOutputStream(file, fileAppend);
        				outWriter = new OutputStreamWriter(fileOut);
        				bufferWriter = new BufferedWriter(outWriter);
        				bufferWriter.write(fileData);
        				bufferWriter.close();
        			}
        			catch(FileNotFoundException exception){
        				exception.printStackTrace();
        			}
        			catch(IOException ioException)
        			{
        				ioException.printStackTrace();
        			}
        			fileAppend = true;
        		}
        		else
        		{
        			if(tempArr[i].equals(BluetoothSynchronize.FILE_NUMBER_MARK))
        			{
        				receiveFileNum = Integer.valueOf(tempArr[i+1]);
        				i++;
        				continue;
        			}
        			/** The mark for a begin of a file */
            		if(tempArr[i].equals(BluetoothSynchronize.SYNCHRONIZE_DATA))
            		{
            			isBeginMark = true;
            			isEndMark = false;
            			
            			fileData = "";
            			
            			filePath = MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username + "/" + tempArr[i+1];
                		file= new File(filePath);
                		i++;
                		continue;
            		}
            		if(tempArr[i].equals(BluetoothSynchronize.FILE_END_MARK))
            		{
            			isEndMark = true;
            			isBeginMark = false;
            			receivedNum ++;
            			int finished;
            			if(receivedNum == receiveFileNum)
            			{
            				finished = 100;
            				isTransferFinished = true;
            			}
            			else
            			{
            				finished = (receivedNum *100)/receiveFileNum; 
            			}
            
            			mHandler.obtainMessage(BluetoothSynchronize.MESSAGE_PROGRESS_BAR, finished, -1, null).sendToTarget();
            			try{
            				fileOut = new FileOutputStream(file, fileAppend);
            				outWriter = new OutputStreamWriter(fileOut);
            				bufferWriter = new BufferedWriter(outWriter);
            				bufferWriter.write(fileData);
            				bufferWriter.close();
            			}
            			catch(FileNotFoundException exception){
            				exception.printStackTrace();
            			}
            			catch(IOException ioException)
            			{
            				ioException.printStackTrace();
            			}
            			fileAppend = false;
            			continue;
            		}
            		if(isBeginMark == true && isEndMark == false)
            		{
            			fileData += tempArr[i];
            			continue;
            		}
        		}
        	}
        }
    }
}