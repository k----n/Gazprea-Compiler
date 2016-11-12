#pragma once

#include "declarations.h"

void neg_b() {
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(!lhs)
}

void neg_i() {
	LOAD(int, lhs, integerValue)
	VALUE_OP(-lhs)
}

void neg_r() {
	LOAD(float, lhs, realValue)
	VALUE_OP(-lhs)
}

void neg_Interval(){
    // VALUE POPPED IS LVALUE so must unwrap
    _unwrap();
    Value* interval1 = stack->pop();

    if (!(interval1)->isInterval()) {
        printf("NOT A VECTOR OR INTERVAL 1\n");
        exit(1);
    }

    Value *sVal = interval1->intervalValue()->get(0);
    stack->push(sVal);
    LOAD(int, interval1_begin, integerValue)

    sVal = interval1->intervalValue()->get(1);
    stack->push(sVal);
    LOAD(int, interval1_end, integerValue)

    int new_begin = 0 - interval1_end;
    int new_end = 0 - interval1_begin;

    Value * node1 = new Value(new_begin);
    Value * node2 = new Value(new_end);

    Vector<Value>* intervalValues = new Vector<Value>;

    intervalValues->append(node1);
    intervalValues->append(node2);

    ValueType* type = new ValueType(IntervalType);

    Value* interval = new Value(type, intervalValues);

    stack -> push(interval);

    interval1 -> release();
    interval -> release();
    type -> release();
}