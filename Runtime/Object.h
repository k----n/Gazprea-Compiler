#pragma once

#include "ObjectType.h"

class Object {
public:	
	virtual void retain() {
		++this->ref_count;
	}
	
	virtual void release() {
		--this->ref_count;
		if (this->ref_count == 0) {
			delete this;
		}
	}
	
	virtual Object* copy() const = 0;
	
protected:
	ObjectType type;
	
	Object(ObjectType type) {
		this->ref_count = 1;
		this->type = type;
	}
	virtual ~Object() {}
	
private:
	int ref_count;
};
