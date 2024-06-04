#ifndef DEBUG_H
#define DEBUG_H
#include "chunk.h"

void dissasembleChunk(Chunk* chunk, const char* name);
int disassembleInstruction(Chunk* chunk, int offset);

#endif //DEBUG_H
