#pragma once

#include "declarations.h"

void div_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs / rhs)
}

void div_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs / rhs)
}

void div_Interval(){
    // VALUE POPPED IS LVALUE so must unwrap
    _unwrap();
    Value* interval1 = stack->pop();

    if (!(interval1)->isInterval()) {
        printf("NOT A VECTOR OR INTERVAL 1\n");
        exit(1);
    }

    _unwrap();
    Value* interval2 = stack->pop();
    if (!(interval2)->isInterval()) {
        printf("NOT A VECTOR OR INTERVAL 2\n");
        exit(1);
    }

    Value *sVal = interval1->intervalValue()->get(0);
    stack->push(sVal);
    LOAD(int, interval1_begin, integerValue)

    sVal = interval1->intervalValue()->get(1);
    stack->push(sVal);
    LOAD(int, interval1_end, integerValue)

    sVal = interval2->intervalValue()->get(0);
    stack->push(sVal);
    LOAD(int, interval2_begin, integerValue)

    sVal = interval2->intervalValue()->get(1);
    stack->push(sVal);
    LOAD(int, interval2_end, integerValue)

    int new_begin = 0;
    int new_end = 0;

    if (interval2_begin > 0){
        double x1y1 = interval1_begin * (1 / (double)interval2_begin);
        double x1y2 = interval1_begin * (1 / (double)interval2_end);
        double x2y1 = interval1_end * (1 / (double)interval2_begin);
        double x2y2 = interval1_end * (1 / (double)interval2_end);

        new_begin = (int)fmin(x1y1,fmin(x1y2,fmin(x2y1,x2y2)));
        new_end = (int)fmax(x1y1,fmax(x1y2,fmax(x2y1,x2y2)));
    }
    else {
        double x1y1_1 = interval1_begin * (1 / (double)interval2_begin);
        double x1y2_1 = interval1_begin * 0;
        double x2y1_1 = interval1_end * (1 / (double)interval2_begin);
        double x2y2_1 = interval1_end * 0;

        double x1y1_2 = interval1_begin * 0;
        double x1y2_2 = interval1_begin * (1 / (double)interval2_end);
        double x2y1_2 = interval1_end * 0;
        double x2y2_2 = interval1_end * (1 / (double)interval2_end);

        new_begin = (int)fmin(x2y2_2,fmin(x2y1_2,fmin(x1y2_2,fmin(x1y1_2,fmin(x1y1_1,fmin(x1y2_1,fmin(x2y1_1,x2y2_1)))))));
        new_end = (int)fmax(x2y2_2,fmax(x2y1_2,fmax(x1y2_2,fmax(x1y1_2,fmax(x1y1_1,fmax(x1y2_1,fmax(x2y1_1,x2y2_1)))))));
    }

    Value * node1 = new Value(new_begin);
    Value * node2 = new Value(new_end);

    Vector<Value>* intervalValues = new Vector<Value>;

    intervalValues->append(node1);
    intervalValues->append(node2);

    ValueType* type = new ValueType(IntervalType);

    Value* interval = new Value(type, intervalValues);

    stack -> push(interval);

    interval1 -> release();
    interval2 -> release();
    interval -> release();
    type -> release();
}