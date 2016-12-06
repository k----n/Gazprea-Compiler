#pragma once

#include "declarations.h"

void dotProduct() {
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if (!(value1)->isVector()){
        if (!(value2)->isVector()){
            printf("Incompatible Vector Equality Types\n");
            exit(1);
        }
        else {
            Vector<Value>* vectorValues = new Vector<Value>();
            int size = value2->vectorValue()->getCount();
            for (int i = 0; i < size; i++){
                vectorValues->append(value1);
            }
            ValueType* newType = new ValueType(VectorType);
            value1 = new Value(newType, vectorValues);
        }
    }
    else if (!(value2)->isVector()){
        if (!(value1)->isVector()){
            printf("Incompatible Vector Equality Types\n");
            exit(1);
        }
        else {
            Vector<Value>* vectorValues = new Vector<Value>();
            int size = value1->vectorValue()->getCount();
            for (int i = 0; i < size; i++){
                vectorValues->append(value2);
            }
            ValueType* newType = new ValueType(VectorType);
            value2 = new Value(newType, vectorValues);
        }
    }

    if (!(value1)->isVector() || !(value2)->isVector()){
        printf("Dot product must be of two vectors\n");
        exit(1);
    }

    int size1 = value1->vectorValue()->getCount();
    int size2 = value2->vectorValue()->getCount();

    if (size1 != size2){
        printf("Dot product must be of two vectors with the same length\n");
        exit(1);
    }

    // only check one vector since the two vectors should have been promoted already
    Value* node = value2->vectorValue()->get(0);
    Value* node2;
    if (node->isInteger()){
        int total = 0;

        for (int i = 0; i < size2; i++){
            node = value2->vectorValue()->get(i);
            node2 = value1->vectorValue()->get(i);

            total += (*(node->integerValue()) * *(node2->integerValue()));
        }

        Value* dp = new Value(total);
        stack -> push(dp);

        return;
    }
    else if (node->isReal()){
        float total = 0;

        for (int i = 0; i < size2; i++){
            node = value2->vectorValue()->get(i);
            node2 = value1->vectorValue()->get(i);

            total += (*(node->realValue()) * *(node2->realValue()));
        }

        Value* dp = new Value(total);
        stack -> push(dp);

        return;
    }
    else {
          printf("Dot product must be of two vectors with numeric base types\n");
          exit(1);
    }
}

void mmMult(){

}