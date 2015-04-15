#define GEN1() gen1(3.141592653589793, -2.718281828459045, 1.4142135623730951, -0.6931471805599453)
__attribute__((noinline)) void gen1(float a, float b, float c, float d) {
  { float r; asm("abs.s %0, %1": "=f" (r): "f" (a)); assertdeq(100, r, 3.141592653589793); }
  { float r; asm("add.s %0, %1, %2": "=f" (r): "f" (a), "f" (b)); assertdeq(101, r, 0.423310825130748); }
  { float r; asm("div.s %0, %1, %2": "=f" (r): "f" (a), "f" (b)); assertdeq(102, r, -1.1557273497909217); }
  { float r; asm("madd.s %0, %1, %2, %3": "=f" (r): "f" (a), "f" (b), "f" (c)); assertdeq(103, r, -0.7026383745693239); }
  { float r; asm("mul.s %0, %1, %2": "=f" (r): "f" (a), "f" (b)); assertdeq(104, r, -8.539734222673566); }
  { float r; asm("neg.s %0, %1": "=f" (r): "f" (a)); assertdeq(105, r, -3.141592653589793); }
  { float r; asm("recip.s %0, %1": "=f" (r): "f" (a)); assertdeq(106, r, 0.3183098861837907); }
  { float r; asm("rsqrt.s %0, %1": "=f" (r): "f" (a)); assertdeq(107, r, 0.5641895835477563); }
  { float r; asm("sqrt.s %0, %1": "=f" (r): "f" (a)); assertdeq(108, r, 1.7724538509055159); }
  { float r; asm("sub.s %0, %1, %2": "=f" (r): "f" (a), "f" (b)); assertdeq(109, r, 5.859874482048838); }
}
