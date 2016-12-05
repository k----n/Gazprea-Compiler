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
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if (!(value1)->isVector() || !(value2)->isVector()) {
        printf("Incompatible Vector Equality Types\n");
        exit(1);
    }

    bool status = true;

    int size1 = value1->vectorValue()->getCount();
    int size2 = value2->vectorValue()->getCount();

    if (size1 != size2){
        printf("Two vectors must be the same length\n");
        exit(1);
    }

    Value* node = value2->vectorValue()->get(0);
    if (node->isInteger()){
        for (int i = 0; i < size2; i++){
            if (*(value2->vectorValue()->get(i)->integerValue()) != *(value1->vectorValue()->get(i)->integerValue())){
                status = false;
            }
        }
    } else if (node->isReal()){
        for (int i = 0; i < size2; i++){
            if (*(value2->vectorValue()->get(i)->realValue()) != *(value1->vectorValue()->get(i)->realValue())){
                status = false;
            }
        }
    } else if (node->isBoolean()){
        for (int i = 0; i < size2; i++){
            if (*(value2->vectorValue()->get(i)->booleanValue()) != *(value1->vectorValue()->get(i)->booleanValue())){
                status = false;
            }
        }
    } else {
        printf("CANNOT EQUATE THIS TYPE\n");
        exit(1);
    }

    Value* result = new Value(status);
    stack -> push(result);
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

void eq_tuple(){
    _unwrap();
    Value* value1 = stack->pop();
    _unwrap();
    Value* value2 = stack->pop();

    if (!(value1)->isTuple() || !(value2)->isTuple()) {
        printf("Incompatible Tuple Equality Types\n");
        exit(1);
    }

    int size1 = value1->tupleValue()->getCount();
    int size2 = value2->tupleValue()->getCount();

    if (size1 != size2){
        printf("Two tuples must be the same length\n");
        exit(1);
    }

    bool status = true;

   for (int i = 0; i < size2; i++){

        Value* t1 = value2->tupleValue()->get(i);
        Value* t2 = value1->tupleValue()->get(i);

        ValueType* type = t1->getType();
        if (type->getType() != t2->getType()->getType()) {
            status = false;
            break;
        }
        else {
            // dive in deeeper
            stack->push(t1);
            stack->push(t2);
            Value * r;
            switch(type->getType()){
                case IntervalType:
                    eq_Interval();
                    break;
                case BooleanType:
                    eq__b();
                    break;
                case IntegerType:
                    eq__i();
                    break;
                case RealType:
                    eq__r();
                    break;
                case VectorType:
                    eq__v();
                    break;
                case CharacterType:
                case NullType:
                case IdentityType:
                case StandardOut:
                case StandardIn:
                case Lvalue:
                case TupleType:
				case MatrixType:
                case StartVector:
                    printf("Type cannot be compared in tuple\n"); exit(1);
            }
            r = stack->pop();
            if (!(r->booleanValue())){
                status = false;
                break;
            }
			
			r -> release();
        }
	   
		t1 -> release();
		t2 -> release();
		type -> release();
    }

    Value* booleanV = new Value(status);

    stack -> push(booleanV);

    booleanV ->release();
    value1 -> release();
    value2 -> release();
}
