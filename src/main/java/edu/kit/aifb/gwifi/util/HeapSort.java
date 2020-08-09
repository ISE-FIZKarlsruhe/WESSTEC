package edu.kit.aifb.gwifi.util;

/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is HeapSort.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 */
/**
 * An implementation of the heap sort algorithm as described in Cormen et al. 
 * Introduction to Algorithms. In addition, this class may sort a part 
 * of an array in ascending order, so that the maximum N elements of the 
 * array are placed in the array's last N positions (the maximum entry of 
 * the array will be in array[array.length-1], the second maximum in 
 * array[array.length-2] etc. 
 * @author Vassilis Plachouras
 * @version $Revision: 1.15 $
 */
public class HeapSort {
	/** The size of the heap.*/
	private static int heapSize;
	/** The left child.*/
	private static int l;
	/** The right child.*/
	private static int r;
	/** The largest.*/
	private static int largest;
	/** A temporary double.*/
	private static double tmpDouble;
	/** A temporary float.*/
	private static float tmpFloat;
	/** A temporary int.*/
	private static int tmpInt;
	
	/**
	 * Builds a maximum heap.
	 * @param A int[] the array which will be transformed into a heap.
	 */
	private static void buildMaxHeap(double[] A, int[] B) {
		heapSize = A.length;
		for (int i = (int) Math.floor(heapSize / 2.0D); i > 0; i--)
			maxHeapify(A, B, i);
	}
	
	/**
	 * Builds a maximum heap.
	 * @param A int[] the array which will be transformed into a heap.
	 */
	private static void buildMaxHeap(float[] A, int[] B) {
		heapSize = A.length;
		for (int i = (int) Math.floor(heapSize / 2.0D); i > 0; i--)
			maxHeapify(A, B, i);
	}
	
	/**
	 * Sorts the given array using heap-sort in ascending order
	 * @param A int[] the array to be sorted
	 */
	public static void heapSort(double[] A, int[] B) {
		buildMaxHeap(A, B);
		for (int i = A.length; i > 0; i--) {
			tmpDouble = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpDouble;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			heapSize--;
			maxHeapify(A, B, 1);
		}
	}
	/**
	 * Sorts the top <tt>topElements</tt> of the given array in 
	 * ascending order using heap sort.
	 * @param A int[] the array to be sorted
	 * @param topElements int the number of elements to be sorted.
	 */
	public static void heapSort(double[] A, int[] B, int topElements) {
		buildMaxHeap(A, B);
		final int end = A.length - topElements;
		
		for (int i = A.length; i > end; i--) {
			tmpDouble = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpDouble;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			heapSize--;
			maxHeapify(A, B, 1);
		}
	}
	/**
	 * Maintains the heap property.
	 * @param A int[] The array on which we operate.
	 * @param i int a position in the array. This number is 
	 * between 1 and A.length inclusive.
	 */
	private static void maxHeapify(double[] A, int[] B, int i) {
		l = 2 * i;
		r = 2 * i + 1;
		if (l <= heapSize && A[l - 1] > A[i - 1])
			largest = l;
		else
			largest = i;
		if (r <= heapSize && A[r - 1] > A[largest - 1])
			largest = r;
		if (largest != i) {
			tmpDouble = A[largest - 1];
			A[largest - 1] = A[i - 1];
			A[i - 1] = tmpDouble;
			tmpInt = B[largest - 1];
			B[largest - 1] = B[i - 1];
			B[i - 1] = tmpInt;
			maxHeapify(A, B, largest);
		}
	}
	
	
	/**
	 * Sorts the given array using heap-sort in ascending order
	 * @param A int[] the array to be sorted
	 */
	public static void heapSort(float[] A, int[] B) {
		buildMaxHeap(A, B);
		for (int i = A.length; i > 0; i--) {
			tmpFloat = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpFloat;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			heapSize--;
			maxHeapify(A, B, 1);
		}
	}
	/**
	 * Sorts the top <tt>topElements</tt> of the given array in 
	 * ascending order using heap sort.
	 * @param A int[] the array to be sorted
	 * @param topElements int the number of elements to be sorted.
	 */
	public static void heapSort(float[] A, int[] B, int topElements) {
		buildMaxHeap(A, B);
		final int end = A.length - topElements;
		
		for (int i = A.length; i > end; i--) {
			tmpFloat = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpFloat;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			heapSize--;
			maxHeapify(A, B, 1);
		}
	}
	/**
	 * Maintains the heap property.
	 * @param A int[] The array on which we operate.
	 * @param i int a position in the array. This number is 
	 * between 1 and A.length inclusive.
	 */
	private static void maxHeapify(float[] A, int[] B, int i) {
		l = 2 * i;
		r = 2 * i + 1;
		if (l <= heapSize && A[l - 1] > A[i - 1])
			largest = l;
		else
			largest = i;
		if (r <= heapSize && A[r - 1] > A[largest - 1])
			largest = r;
		if (largest != i) {
			tmpFloat = A[largest - 1];
			A[largest - 1] = A[i - 1];
			A[i - 1] = tmpFloat;
			tmpInt = B[largest - 1];
			B[largest - 1] = B[i - 1];
			B[i - 1] = tmpInt;
			maxHeapify(A, B, largest);
		}
	}
}
