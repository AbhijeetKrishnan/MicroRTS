s3(["for (each unit u)"|A], B) :-
   s4(A, B).

s5(["not"|A], B) :-
   b(A, B).
s5(A, B) :-
   b(A, B).

b(["b1"|A], A).
b(["b2"|A], A).
b(["b3"|A], A).

s1(A, B) :-
   A=B.
s1(A, B) :-
   c(A, C),
   s1(C, B).
s1(A, B) :-
   s2(A, C),
   s1(C, B).
s1(A, B) :-
   s3(A, C),
   s1(C, B).

c(A, B) :-
   A=B.
c(["c1"|A], A).
c(["c1"|A], B) :-
   c(A, B).
c(["c2"|A], A).
c(["c2"|A], B) :-
   c(A, B).
c(["c3"|A], A).
c(["c3"|A], B) :-
   c(A, B).

s2(["if"|A], B) :-
   s5(A, C),
   C=["then"|D],
   c(D, B).
s2(["if"|A], B) :-
   b(A, C),
   C=["then"|D],
   c(D, E),
   E=["else"|F],
   c(F, B).

s4(A, B) :-
   A=B.
s4(A, B) :-
   c(A, C),
   s4(C, B).
s4(A, B) :-
   s2(A, C),
   s4(C, B).

clause(C) :- s1(C, []).

{ clause(C) } :- length(C, 3).

#show clause/1.