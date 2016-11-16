#pragma once

void swapStackTopTwo() {
	Value* topValue = stack->pop();
	Value* secondValue = stack->pop();
	stack->push(topValue);
	stack->push(secondValue);
	topValue->release();
	secondValue->release();
}

void promoteTo_b() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	int* intValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
	switch (type->getType()) {
		case NullType:
			newValue = new Value(false);
			break;
		case IdentityType:
			newValue = new Value(true);
			break;
		case BooleanType:
			newValue = value;
			newValue->retain();
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value(*intValue != 0);
			break;
		case RealType: printf("Cannot promote Real\n"); exit(1);
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value(*charValue != '\0');
			break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case VectorType: printf("Cannot promote Vector\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_b();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_i() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	float* realValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
	switch (type->getType()) {
		case NullType:
			newValue = new Value(0);
			break;
		case IdentityType:
			newValue = new Value(1);
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1 : 0);
			break;
		case IntegerType:
			newValue = value;
			newValue->retain();
			break;
		case RealType:
			realValue = value->realValue();
			newValue = new Value((int)*realValue);
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((int)*charValue);
			break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case VectorType: printf("Cannot promote Vector\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_i();
		case StartVector: printf("Cannot promote startvector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_r() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
	switch (type->getType()) {
		case NullType:
			newValue = new Value(0.0f);
			break;
		case IdentityType:
			newValue = new Value(1.0f);
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1.0f : 0.0f);
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((float)*intValue);
			break;
		case RealType:
			newValue = value;
			newValue->retain();
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((float)*charValue);
			break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case VectorType: printf("Cannot promote Vector\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_r();
		case StartVector: printf("Cannot promote StartVector"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_c() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	ValueType* type = value->getType();
	switch (type->getType()) {
		case NullType:
			newValue = new Value('\0');
			break;
		case IdentityType:
			newValue = new Value('\1');
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value((char)(*boolValue ? 1 : 0));
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((char)(*intValue % 256));
			break;
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType:
			newValue = value;
			newValue->retain();
			break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
	    case IntervalType: printf("Cannot promote Interval\n"); exit(1);
	    case VectorType: printf("Cannot promote Vector\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_c();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_vector() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	Vector<Value>* vectorValues = nullptr;
	ValueType* newType = nullptr;
	ValueType* type = value->getType();
	Vector<Value>* intervalValues = nullptr;
	Value* node = nullptr;
	int start = 0;
	int end = 0;
	switch (type->getType()) {
        case IntervalType:
            intervalValues = value->intervalValue();
            stack->push(intervalValues->get(0));
            _unwrap();
            start = *(stack->pop()->integerValue());
            stack->push(intervalValues->get(1));
            _unwrap();
            end = *(stack->pop()->integerValue());

            vectorValues = new Vector<Value>;

            for (int i = start; i <= end; i++){
                node = new Value(i);
                vectorValues->append(node);
            }

           	newType = new ValueType(VectorType);

           	newValue = new Value(newType, vectorValues);
            break;
        case VectorType:
			newValue = value;
			newValue->retain();
			break;
		case NullType:
		case IdentityType:
		case BooleanType:
		case IntegerType:
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType:
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_c();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

