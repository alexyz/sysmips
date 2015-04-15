#define GEN1 gen1(3.141592653589793, -2.718281828459045, 1.4142135623730951, -0.6931471805599453)
__attribute__((noinline)) void gen1(float a, float b, float c, float d) {
  { float r; asm("abs.s %0, %1": "=f" (r): "f" (a)); assertdeq(1, r, 3.141592653589793); }
  { float r; asm("add.s %0, %1, %2": "=f" (r): "f" (a), "f" (b)); assertdeq(2, r, 0.423310825130748); }
}
