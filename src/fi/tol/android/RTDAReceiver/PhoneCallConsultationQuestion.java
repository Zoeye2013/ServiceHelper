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
 *  Class PhoneCallConsultationQuestion
 *
 *  Question class, which represent one question in the questionnaire
 *  including question number, the question, answers for the question.
 *  and the logic for the order of questions
 */

package fi.tol.android.RTDAReceiver;


import java.util.ArrayList;

public class PhoneCallConsultationQuestion {
	
	/** Question number */
	private int questionNo;
	
	/** What is the question */
	private String questionTitle;
	
	/** How many answers does the question have */
	private int answerNum;
	
	/** Answers list */
	private ArrayList<String> answersList = new ArrayList<String>();
	
	/** The logic for the order of questions */
	private ArrayList<Integer> directToQuestionList = new ArrayList<Integer>();
	
	/** The user selected answer to the question */
	private Integer selectedAnswerIndex;
	
	public void setQuestionNo(int no)
	{
		questionNo = no;
	}
	public int getQuestionNo()
	{
		return questionNo;
	}
	
	public void setQuestionTitle(String question)
	{
		questionTitle = question;
	}
	public String getQuestionTitle()
	{
		return questionTitle;
	}
	
	public void setAnswerNum(int num)
	{
		answerNum = num;
	}
	public int getAnswerNum()
	{
		return answerNum;
	}
	public void answerNumAdd()
	{
		answerNum ++;
	}
	public void answerNumDel()
	{
		answerNum --;
	}
	public void addAnswersList(String answer)
	{
		answersList.add(answer);
	}
	public void addDirectToQuestionList(int questionNo)
	{
		directToQuestionList.add(questionNo);
	}
	public void setDirectToQuestionList(int answerIndex, int directToQNo)
	{
		directToQuestionList.set(answerIndex, directToQNo);
	}
	public String getAnswer(int answerIndex)
	{
		return answersList.get(answerIndex);
	}
	public void setAnswer(int answerIndex, String answer){
		answersList.set(answerIndex, answer);
	}
	public ArrayList<String> getAnswersList()
	{
		return answersList;
	}
	public ArrayList<Integer> getDirectToList()
	{
		return directToQuestionList;
	}
	public int getDirectToQuestionNo(int answerIndex)
	{
		return directToQuestionList.get(answerIndex);
	}
	public void setSelectedAnswerIndex(int selectedIndex)
	{
		selectedAnswerIndex = selectedIndex;
	}
	public Integer getSelectedAnswerIndex()
	{
		return selectedAnswerIndex;
	}
	public void initSelectedIndex()
	{
		selectedAnswerIndex = null;
	}
}
