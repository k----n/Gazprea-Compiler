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

void neg_v() {
    _unwrap();
    Value* value = stack->pop();

    if (!(value)->isVector()) {
        printf("NOT A VECTOR; CANNOT NEGATE\n");
        exit(1);
    }

    int size = value->vectorValue()->getCount();

    Value* node = value->vectorValue()->get(0);
    Vector<Value>* vectorValues = new Vector<Value>;

    if (node->isInteger()){
        for (int i = 0; i < size; i++){
            node = new Value(-*(value->vectorValue()->get(i)->integerValue()));
            vectorValues->append(node);
        }
    } else if (node->isReal()){
        for (int i = 0; i < size; i++){
            node = new Value(-*(value->vectorValue()->get(i)->realValue()));
            vectorValues->append(node);
        }
    } else if (node->isBoolean()){
        for (int i = 0; i < size; i++){
            node = new Value(!*(value->vectorValue()->get(i)->booleanValue()));
            vectorValues->append(node);
        }
    }
    else {
        printf("CANNOT NEGATE THIS TYPE\n");
        exit(1);
    }

    ValueType* newType = new ValueType(VectorType);
    Value* newValue = new Value(newType, vectorValues);
    stack->push(newValue);
    newValue->release();
    newType -> release();
}

void neg_m() {
    _unwrap();
    Value* value = stack->pop();

    if (!(value)->isMatrix()) {
        printf("NOT A MATRIX; CANNOT NEGATE\n");
        exit(1);
    }

    int size = value->matrixValue()->getCount();

    Vector<Value>* matrixValues = new Vector<Value>;

    for (int i = 0; i < size; i++){
        stack->push(value->matrixValue()->get(i));
        neg_v();
        matrixValues->append(stack->pop()->copy());
    }

    ValueType* newType = new ValueType(MatrixType);
    Value* newValue = new Value(newType, matrixValues);
    stack->push(newValue);
    newValue->release();
    newType -> release();
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