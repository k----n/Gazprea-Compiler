#pragma once

#include "notAllowedImports.h"
#include "declarations.h"

// TODO fix not allowed imports

void exp_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP((int)pow((float)lhs, (float)rhs))
}

void exp_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(powf(lhs, rhs))
}

void exp_v() {
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
                node = new Value((int)pow((float)scalar, (float)*(value1->vectorValue()->get(i)->integerValue())));
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
                node = new Value(powf(scalar, *(value1->vectorValue()->get(i)->realValue())));
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

//        Vector<Value>* vectorValues = new Vector<Value>;

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
                node = new Value((int)pow((float)*(value2->vectorValue()->get(i)->integerValue()), (float)*(value1->vectorValue()->get(i)->integerValue())));
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
                node = new Value(powf(*(value2->vectorValue()->get(i)->realValue()), *(value1->vectorValue()->get(i)->realValue())));
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
            node = new Value((int)pow((float)*(value2->vectorValue()->get(i)->integerValue()), (float)scalar));
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
            node = new Value(powf(*(value2->vectorValue()->get(i)->realValue()), scalar));
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

void exp_m() {
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if (value1->isMatrix()){
        if (value2->isInteger() || value2->isReal()){
            int size = value1->matrixValue()->getCount();
            Vector<Value>* matrixValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                stack->push(value1->matrixValue()->get(i));
                stack->push(value2);
                exp_v();
                matrixValues->append(stack->pop()->copy());
            }
            ValueType* newType = new ValueType(MatrixType);
            Value* newValue = new Value(newType, matrixValues);
            stack->push(newValue);
            return;
        } else if (value2->isMatrix()){
            int size1 = value1->matrixValue()->getCount();
            int size2 = value2->matrixValue()->getCount();
            if (size1 != size2){
                printf("Two matrix must be the same length\n");
                exit(1);
            }
            Vector<Value>* matrixValues = new Vector<Value>;
            for (int i = 0; i < size1; i++){
                stack->push(value1->matrixValue()->get(i));
                stack->push(value2->matrixValue()->get(i));
                exp_v();
                matrixValues->append(stack->pop()->copy());
            }
            ValueType* newType = new ValueType(MatrixType);
            Value* newValue = new Value(newType, matrixValues);
            stack->push(newValue);
            return;
        } else {
            printf("Incompatible Matrix exponentiation types\n");
            exit(1);
        }
    } else if (value2->isMatrix()){
        if (value1->isInteger() || value1->isReal()){
            int size = value2->matrixValue()->getCount();
            Vector<Value>* matrixValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                stack->push(value1);
                stack->push(value2->matrixValue()->get(i));
                exp_v();
                matrixValues->append(stack->pop()->copy());
            }
            ValueType* newType = new ValueType(MatrixType);
            Value* newValue = new Value(newType, matrixValues);
            stack->push(newValue);
            return;
        } else {
            printf("Incompatible Matrix exponentiation types\n");
            exit(1);
        }
    } else {
        printf("Incompatible Matrix exponentiation types\n");
        exit(1);
    }
}

