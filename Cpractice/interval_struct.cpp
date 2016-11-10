#include <iostream>
#include <stdio.h>
using namespace std;

struct interval {
    int begin;
    int end;
};

int main() {
    interval interval1;
    interval1.begin = 1;
    interval1.end = 3;

    printf("%d\n",interval1.begin);
    printf("%d\n",interval1.end);

    return 0;
}
