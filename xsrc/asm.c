#define NI __attribute__((noinline))
typedef union { int i; float f; long long l; double d; } result_t;

NI float testabss (float x) {
  float r;
  asm("abs.s %0, %1" : "=f" (r) : "f" (x));
  return r;
}

NI double testabsd (double x) {
  double r;
  asm("abs.d %0, %1" : "=f" (r) : "f" (x));
  return r;
}

void __start (result_t *p) {
  p[0].f = testabss(-3.14);
  p[1].d = testabsd(-3.14);
  asm("break");
}
