#include <iostream>

struct tuple {
  int a;
  int b;
  char c;
  int d;
};

int main() {
  struct tuple *alg = new struct tuple();

  alg->a = 1;
  alg->b = 2;
  alg->c = 'a';
  alg->d = 3;

  int x = alg->a;
  int y = alg->b;
  char z = alg->c;
  int a = alg->d;
}
