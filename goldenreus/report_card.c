#include<stdio.h>

int main()
{
	int kor, eng, math;
	int sum = 0;

	math = 80;
	eng = 100;
	kor = 90;
	sum = 80 + 100 + 90;

	printf("This program print report card.\n");

	printf("Korean : %d\n", kor);
	printf("English : %d\n", eng);
	printf("Math : %d\n", math);
	printf("Sum : %d\n", sum);
	printf("Average : %d\n", sum/3);
	return 0;
}
