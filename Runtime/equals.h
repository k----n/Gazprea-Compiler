#pragma once

#include "declarations.h"

void eq__b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs == rhs)
}

void eq__i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs == rhs)
}

void eq__r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs == rhs)
}

void eq__v() {
}

void eq_Interval(){
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

    Value* booleanValue;

    if ((interval1_begin == interval2_begin) && (interval1_end == interval2_end)){
        booleanValue = new Value(true);
    }
    else {
        booleanValue = new Value(false);
    }

    stack -> push(booleanValue);

    interval1 -> release();
    interval2 -> release();
}