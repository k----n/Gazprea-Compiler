#pragma once

#include "declarations.h"

void lor_b() {
	LOAD(bool, rhs, booleanValue)
	LOAD(bool, lhs, booleanValue)
	VALUE_OP(lhs || rhs)
}

void lor_v() {
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if ((value1)->isVector()) {
        // CASE: vector + (scalar or vector)
        if (value2->isBoolean()){
            int size = value1->vectorValue()->getCount();

            Vector<Value>* vectorValues = new Vector<Value>;

            // add scalar to each element value2

            bool scalar = *(value2->booleanValue());

            Value* node;
            for (int i = 0; i < size; i++){
                node = new Value(scalar || *(value1->vectorValue()->get(i)->booleanValue()));
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
        if (node->isBoolean()){
            Vector<Value>* vectorValues = new Vector<Value>;

            for (int i = 0; i < size2; i++){
                node = new Value(*(value2->vectorValue()->get(i)->booleanValue()) || *(value1->vectorValue()->get(i)->booleanValue()));
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
              printf("Vectors must have the same base types\n");
              exit(1);
        }
    }
    else if (value1->isBoolean()){
        if (!(value2)->isVector()){
            printf("Incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        bool scalar = *(value1->booleanValue());

        int size2 = value2->vectorValue()->getCount();
        Value* node;
        for (int i = 0; i < size2; i++){
            node = new Value(*(value2->vectorValue()->get(i)->booleanValue()) || scalar);
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
