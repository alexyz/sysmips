#include <math.h>

__attribute__((noinline)) void assertdeq (int testid, double d1, double d2) {
  double c = d1 - d2;
  // 1/256
  if (c < -0.00390625 || c > 0.00390625) {
    asm("break 1");
  }
}

#include "gen1.c"

void __start () {
  GEN1;
  asm("break 0");
}
