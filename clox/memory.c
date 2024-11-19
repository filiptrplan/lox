#include <stdlib.h>

#include "memory.h"
#include "vm.h"
#include "object.h"

/**
 * This is the main function used by clox to manage memory. The rules are used when allocating:
 * (oldSize, newSize) 
 * (0, >0) -> Allocate a new block
 * (>0, 0) -> Free allocation
 * (>0, <oldSize) -> Shrink existing allocation
 * (>0, >oldSize) -> Grow existing allocation
 * @param pointer Pointer to reallocate
 * @param oldSize Previous size of pointer
 * @param newSize Desired size of pointer
 * @return A pointer to the reallocated memory
 */
void* reallocate(void* pointer, size_t oldSize, size_t newSize) {
    if (newSize == 0) {
        free(pointer);
        return NULL;
    }

    void* result = realloc(pointer, newSize);
    if (result == NULL) exit(1); // Alloctation failed
    return result;
}

static void freeObject(Obj* object) {
    switch (object->type) {
        case OBJ_STRING:
            ObjString* objString = (ObjString*)object;
            FREE_ARRAY(char, objString->chars, objString->length + 1);
            FREE(ObjString, objString);
            break;
        case OBJ_FUNCTION:
            ObjFunction* objFunction = (ObjFunction*)object;
            freeChunk(&objFunction->chunk);
            FREE(ObjFunction, objFunction);
            break;
        case OBJ_NATIVE:
            FREE(ObjNative, object);
            break;
        case OBJ_CLOSURE:
            FREE(ObjClosure, object);
            break;
    }
}

void freeObjects() {
    Obj* object = vm.objects;
    while (object != NULL) {
        Obj* next = object->next;
        freeObject(object);
        object = next;
    }
}
