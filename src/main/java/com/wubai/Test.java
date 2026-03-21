package com.wubai;

import java.util.Arrays;


public class Test {
    public static void main(String[] args) {
        countAndPrint(new int[]{-1,2,1,-4},  1);
        countAndPrint(new int[]{0,0,0},  1);
        countAndPrint(new int[]{1,2,3,4,5},  1);
        countAndPrint(new int[]{1,2,3,4,5},  5);
        countAndPrint(new int[]{-1,2,1,-1},  0);

    }

    public static void countAndPrint(int[] nums, int target) {
        int[][] arr = new int[nums.length][2];
        for(int i = 0; i < nums.length; i++){
            arr[i][0] = nums[i];
            arr[i][1]  = Math.abs(nums[i] - target );
        }
        Arrays.sort(arr , (a,b) -> a[1]- b[1]);
        System.out.println(arr[0][0]+arr[1][0]+arr[2][0]);
    }
}
