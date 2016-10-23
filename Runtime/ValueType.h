#pragma once

#include "Object.h"
#include "BuiltinType.h"

class ValueType : public Object {
public:
	ValueType(BuiltinType type) : super(TypeValueType) {
		this->builtinType = type;
	}
		
	virtual ValueType* copy() const {
		return new ValueType(this->builtinType);
	}
	
	BuiltinType getType() { return this->builtinType; }
	
private:
	BuiltinType builtinType;
	
	virtual ~ValueType() {}
	
	typedef Object super;
};
