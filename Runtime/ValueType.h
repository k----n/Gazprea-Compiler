#pragma once

#include "Object.h"
#include "BuiltinType.h"

class ValueType : public Object {
public:
	ValueType(BuiltinType type) : super(TypeValueType) {
		this->builtinType = type;
		has_vector_size = false;
		has_matrix_size = false;
	}
		
	virtual ValueType* copy() const {
		return new ValueType(this->builtinType);
	}
	
	BuiltinType getType() { return this->builtinType; }

	void setVectorSize(int size) {
	    if (this->builtinType != VectorType) {
            throw "not a vector type";
        }

	    this->vector_size = size;
	    this->has_vector_size = true;
	}

	void setMatrixSize(int size) {
	    if (this->builtinType != VectorType) {
            throw "not a vector type";
        }

	    this->matrix_size = size;
	    this->has_matrix_size = true;
	}

	void setContainedType(BuiltinType type) {
	    this->containedType = type;
	}

	int getVectorSize(int size) {
	    if (this->builtinType != VectorType) {
	        throw "not a vector type";
	    }

	    if (this->has_vector_size) {
	        return this->vector_size;
	    } else {
	        return -1;
	    }
	}

	int getMatrixSize(int size) {
	    if (this->builtinType != VectorType) {
            throw "not a vector type";
        }

	    if (this->has_matrix_size) {
	        return this->matrix_size;
	    } else {
	        return -1;
	    }
	}


private:
	BuiltinType builtinType;

	// for container objects: matrix and vector
	BuiltinType containedType;
	int vector_size;
	bool has_vector_size;
	int matrix_size;
	bool has_matrix_size;

	virtual ~ValueType() {}
	
	typedef Object super;
};
