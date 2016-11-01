#include <stdio.h>

struct tuple {
  int a;
  int b;
  char c;
  int d;
};



int main() {
  struct tuple alg = {1,2,'a',3};
  int x = alg.a;
  int y = alg.b;
  char z = alg.c;
  int a = alg.d;
}
