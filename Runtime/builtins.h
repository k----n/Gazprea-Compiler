#pragma once

void std_output()	{ pushEmptyValue(StandardOut);	}
void std_input()	{ pushEmptyValue(StandardIn);	}

void stream_state() {
	_unwrap();
	Value* stream = stack->pop();
	
	Value* value;
	switch (stream->getType()->getType()) {
		case NullType:
		case IdentityType:
		case BooleanType:
		case IntegerType:
		case RealType:
		case CharacterType:
		case TupleType:
		case IntervalType:
		case VectorType:
		case MatrixType:
		case StandardOut:
		case Lvalue:
		case StartVector:
			printf("Argument was not a stream");
			exit(1);
		case StandardIn:
			value = ((Value*)stream->extra_data())->copy();
	}
	stack->push(value);
	value->release();
	stream->release();
}

void length() {
	_unwrap();
	Value* vectorValue = stack->pop();
	Vector<Value>* vector = vectorValue->vectorValue();
	Value* length = new Value(vector->getCount());
	stack->push(length);
	//length->release();
	//vector->release();
	//vectorValue->release();
}


void rows() {
	_unwrap();
	Value* matrixValue = stack->pop();
	Vector<Value>* matrix = matrixValue->matrixValue();
	Value* length = new Value(matrix->getCount());
	stack->push(length);
	//length->release();
	//matrix->release();
	//matrixValue->release();
}

void columns() {
	_unwrap();
	Value* matrixValue = stack->pop();
	Vector<Value>* matrix = matrixValue->matrixValue();
	Value* length = new Value(matrix->getCount());
	if (length > 0) {
		Value* rowValue = matrix->get(0);
		Vector<Value>* row = rowValue->vectorValue();
		Value* length = new Value(row->getCount());
		stack->push(length);
		//length->release();
		//row->release();
		//rowValue->release();
	}
	//length->release();
	//matrix->release();
	//matrixValue->release();
}

void reverse() {
	_unwrap();
	Value* vectorValue = stack->pop();
	Vector<Value>* vector = vectorValue->vectorValue();
	
	Vector<Value>* reversedVector = new Vector<Value>();
	for (int i = 0; i < vector->getCount(); ++i) {
		Value* tmp = vector->get(vector->getCount() - 1 - i);
		Value* copy = tmp->copy();
		tmp->release();
		reversedVector->append(copy);
		copy->release();
	}
	
	ValueType* type = vectorValue->getType();
	Value* reversedValue = new Value(type, reversedVector);
	stack->push(reversedValue);
	//type->release();
	
	//vector->release();
	//vectorValue->release();
}
