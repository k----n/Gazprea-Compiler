#pragma once

#include "declarations.h"

void concat_vect(){
    // VALUE POPPED IS LVALUE so must unwrap
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if ((value1)->isVector()) {
        // CASE: vector || (scalar or vector)
        if (value2->isBoolean() || value2->isInteger() || value2->isReal() || value2->isCharacter()){
            // add scalar to vector

            int size = value1->vectorValue()->getCount();

            Vector<Value>* vectorValues = new Vector<Value>;

            // add scalar
            vectorValues->append(value2);

	        Value* node;
            for (int i = 0; i < size; i++){
                node = value1->vectorValue()->get(i);
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
            printf("Concatenating incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        // add vector to vector
        int size1 = value1->vectorValue()->getCount();
        int size2 = value2->vectorValue()->getCount();

        Value* node;
        for (int i = 0; i < size2; i++){
            node = value2->vectorValue()->get(i);
            vectorValues->append(node);
        }
        for (int i = 0; i < size1; i++){
            node = value1->vectorValue()->get(i);
            vectorValues->append(node);
        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;
    }
    else if (value1->isBoolean() || value1->isInteger() || value1->isReal() || value1->isCharacter()){
        // CASE: scalar || vector
        if (!(value2)->isVector()){
            printf("Concatenating incompatible types\n");
            exit(1);
        }

        Vector<Value>* vectorValues = new Vector<Value>;

        int size2 = value2->vectorValue()->getCount();
        Value* node;
        for (int i = 0; i < size2; i++){
            node = value2->vectorValue()->get(i);
            vectorValues->append(node);
        }

        // add scalar to vector
        vectorValues->append(value1);

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, vectorValues);
        stack->push(newValue);
        newValue->release();
        newType -> release();
        return;
    }
    else {
        printf("Concatenating incompatible types\n");
        exit(1);
    }
}