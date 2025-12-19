.class public Main
.super java/lang/Object

.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method

.method public static Point_init(III)I
   .limit stack 100
   .limit locals 100
Point_init:
   iload 1
   istore 3
   iload 0
   iload 3
   putfield Main/x I
   iload 2
   istore 4
   iload 0
   iload 4
   putfield Main/y I
   return
.end method

.method public static Point_sum(I)I
   .limit stack 100
   .limit locals 100
Point_sum:
   iload 0
   getfield Main/x I
   istore 1
   iload 0
   getfield Main/y I
   istore 2
   iload 1
   iload 2
   iadd
   istore 3
   iload 3
   ireturn
.end method

.method public static main([Ljava/lang/String;)V
   .limit stack 100
   .limit locals 100
main:
   new Point
   dup
   invokespecial Point/<init>()V
   istore 0
   iload 0
   istore 1
   iload 1
   istore 2
   iload 2
   bipush 10
   istore 3
   bipush 20
   istore 4
   iload 3
   iload 4
   invokestatic Main/Point_init(III)I
   istore 5
   iload 1
   istore 6
   iload 6
   invokestatic Main/Point_sum(I)I
   istore 7
   iload 7
   istore 8
   return
.end method
