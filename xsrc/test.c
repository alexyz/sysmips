#include <math.h>

__attribute__((noinline)) void assertdeq (int testid, double d1, double d2) {
  double c = d1 - d2;
  // 1/64
  if (c < -0.015625 || c > 0.015625) {
    asm("break 1");
  }
}

#include "gen1def.c"

void __start () {
  #include "gen1.c"
  asm("break 0");
}
