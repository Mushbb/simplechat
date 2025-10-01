#include <stdio.h>
#include <math.h>
#include "YachtScore.h"

// 모든 경우의 수의 최대점수 / 경우의 수 갯수
// hold : 이진수 5자리로 어느자리가 set인지 체크 (1의자리가 첫주사위)
double avgScore(int dice[DICE], int hold);

// 모든 홀드를 넣어보고 평균점수가 가장 높은 홀드를 선택함
int decideHold(int dice[DICE]);

void displayHold(int hold);

// 샘플 주사위로 기본동작 테스트 진행
// 보너스, 어떤 족보가 막혔는지, 몇번째 롤인지, 상대와의 점수차 등등은 추가로 예외처리 필요
int main() {
	int dice[DICE] = { 5, 1, 0, 2, 4 };	// 샘플: 0이 1주사위
	YachtScore* scores = calcScore(dice);
	displayScore(scores);
	
	int hold = decideHold(dice);
	displayHold(hold);
	printf("%f\n%f", avgScore(dice, 16 + 4 + 1), avgScore(dice, hold));
	
	free(scores);
}

double avgScore(int dice[DICE], int hold) {
	int base[DICE] = { -1 };
	int b = 0;
	// 고정된 주사위는 그냥 넣어둠
	for (int i = 0; i < DICE; i++) {
		if (hold % 2)
			base[b++] = dice[i];
		hold /= 2;
	}

	// 남은 주사위는 모든 경우의 수 체크
	// 6진수를 +1해가면서 5로 꽉차면 종료
	int sum = 0, cases = 0;
	const int end = (int)pow(6, (DICE - b));
	for (int i = 0; i < end; i++) {
		int t = i;
		for (int j = b; j < DICE; j++) {
			base[j] = t % 6;
			t /= 6;
		}
		sum += maxScore(base);
		cases++;
	}

	return (double)sum / cases;
}

int decideHold(int dice[DICE]) {
	double max = 0.0;
	int maxHold = -1;
	const int end = (int)pow(2, DICE);

	// 모든 경우의 수를 다 넣어보고 평균점수가 제일 높은 홀드를 선택
	for (int i = 0; i < end; i++) {
		double avg = avgScore(dice, i);
		if (avg > max) {
			max = avg;
			maxHold = i;
		}
	}

	return maxHold;
}

void displayHold(int hold) {
	for (int i = 0; i < 5; i++) {
		printf("%d ", hold % 2);
		hold /= 2;
	}
	printf("\n");
}