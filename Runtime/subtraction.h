#pragma once

#include "declarations.h"

void sub_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs - rhs)
}

void sub_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs - rhs)
}

void sub_v() {
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if ((value1)->isVector()) {
        // CASE: vector + (scalar or vector)
        if (value2->isInteger()){
            // add scalar to vector

            int size = value1->vectorValue()->getCount();

            Vector<Value>* vectorValues = new Vector<Value>;

            // add scalar to each element value2
            int scalar = *(value2->integerValue());

            Value* node;
            for (int i = 0; i < size; i++){
                node = new Value(scalar - *(value1->vectorValue()->get(i)->integerValue()));
                vectorValues->append(node);
            }

            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, vectorValues);
            stack->push(newValue);
            newValue->release();
            newType -> release();
            return;
        }
        else if (value2->isReal()){
            // add scalar to vector

            int size = value1->vectorValue()->getCount();

            Vector<Value>* vectorValues = new Vector<Value>;

            // add scalar to each element value2

            float scalar = *(value2->realValue());

            Value* node;
            for (int i = 0; i < size; i++){
                node = new Value(scalar - *(value1->vectorValue()->get(i)->realValue()));
                vectorValues->append(node);
            }

            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, vectorValues);
            stack->push(newValue);
            newValue->release();
            newType -> release();
            return;
        }
        else if (!(value2)->isVector()){
            printf("Incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        // add vector to vector
        int size1 = value1->vectorValue()->getCount();
        int size2 = value2->vectorValue()->getCount();

        if (size1 != size2){
            printf("Two vectors must be the same length\n");
            exit(1);
        }

        // only check one vector since the two vectors should have been promoted already
        Value* node = value2->vectorValue()->get(0);
        if (node->isInteger()){
            Vector<Value>* vectorValues = new Vector<Value>;

            for (int i = 0; i < size2; i++){
                node = new Value(*(value2->vectorValue()->get(i)->integerValue()) - *(value1->vectorValue()->get(i)->integerValue()));
                vectorValues->append(node);
            }

            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, vectorValues);
            stack->push(newValue);
            newValue->release();
            newType -> release();
            return;
        }
        else if (node->isReal()){
            Vector<Value>* vectorValues = new Vector<Value>;

            for (int i = 0; i < size2; i++){
                node = new Value(*(value2->vectorValue()->get(i)->realValue()) - *(value1->vectorValue()->get(i)->realValue()));
                vectorValues->append(node);
            }

            ValueType* newType = new ValueType(VectorType);
            Value* newValue = new Value(newType, vectorValues);
            stack->push(newValue);
            newValue->release();
            newType -> release();
            return;
        }
        else {
              printf("Vectors must have the same numeric base types\n");
              exit(1);
        }
    }
    else if (value1->isInteger()){
        // CASE: scalar + vector
        if (!(value2)->isVector()){
            printf("Incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        int scalar = *(value1->integerValue());

        int size2 = value2->vectorValue()->getCount();
        Value* node;
        for (int i = 0; i < size2; i++){
            node = new Value(*(value2->vectorValue()->get(i)->integerValue()) - scalar);
            vectorValues->append(node);
        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;
    }
    else if (value1->isReal()){
        if (!(value2)->isVector()){
            printf("Incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        float scalar = *(value1->realValue());

        int size2 = value2->vectorValue()->getCount();
        Value* node;
        for (int i = 0; i < size2; i++){
            node = new Value(*(value2->vectorValue()->get(i)->realValue()) - scalar);
            vectorValues->append(node);
        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;
    }
    else {
        printf("Incompatible types\n");
        exit(1);
    }
}

void sub_Interval(){
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

    int new_begin = interval1_begin - interval2_end;
    int new_end = interval1_end - interval2_begin;

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