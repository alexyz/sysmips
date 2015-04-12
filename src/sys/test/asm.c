// xgcc -Wall -nostartfiles asm.c
// xobjdump -d a.out

static float testabs1;
static double testabs2;

void testabs (void) {
  double x = -3.14159;
  asm("abs.s %0, %1" : "=f" (testabs1) : "f" (-3.14159));
}

void __start (void *result) {
  testabs();
}
