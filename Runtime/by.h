#pragma once

#include "declarations.h"

void byInterval(int index) {
    // TODO LOOK OVER THIS
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

    Vector<Value>* intervalValues = new Vector<Value>;

    Value * node;
    while (interval1_begin <= interval1_end){
        node = new Value(interval1_begin);
        intervalValues->append(node);
        interval1_begin+=index;
    }

    ValueType* type = new ValueType(VectorType);

    Value* vector = new Value(type, intervalValues);

    stack -> push(vector);

    interval1 -> release();
    vector -> release();
    type -> release();
}