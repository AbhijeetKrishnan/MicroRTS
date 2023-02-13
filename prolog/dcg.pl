% s-->[].
% s-->[a],s,[b].

% s1 --> [].
% s1 --> c, s1.
% s1 --> s2, s1.
% s1 --> s3, s1.

% s2 --> ["if"], s5, ["then"], c.
% s2 --> ["if"], b, ["then"], c, ["else"], c.

% s3 --> ["for (each unit u)"], s4.

% s4 --> [].
% s4 --> c, s4.
% s4 --> s2, s4.

% s5 --> ["not"], b.
% s5 --> b.

% b --> ["b1"].
% b --> ["b2"].
% b --> ["b3"].

% c --> [].
% c --> ["c1"].
% c --> ["c1"], c.
% c --> ["c2"].
% c --> ["c2"], c.
% c --> ["c3"].
% c --> ["c3"], c.

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