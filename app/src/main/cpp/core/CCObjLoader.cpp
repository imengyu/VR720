#include "CCObjLoader.h"
#include "CCMesh.h"
#include <vector>


bool CCObjLoader::Load(const char* path, CCMesh* mesh)
{
    if(mesh == nullptr || path == nullptr) {
        return false;
    }

    FILE* file = fopen(path, "r");
    if (file == nullptr) {
        LOGEF(LOG_TAG, "Open %s failed !", path);
        SetLastError("Open file failed");
        return false;
    }

    while (true) {

        char lineHeader[128];
        int res = fscanf(file, "%s", lineHeader);
        if (res == EOF)
            break; 

        if (strcmp(lineHeader, "v") == 0) {
            glm::vec3 vertex;
            fscanf(file, "%f %f %f\n", &vertex.x, &vertex.y, &vertex.z);
            mesh->positions.push_back(vertex);
        }
        else if (strcmp(lineHeader, "vt") == 0) {
            glm::vec2 uv;
            fscanf(file, "%f %f\n", &uv.x, &uv.y);
            mesh->texCoords.push_back(uv);
        }
        else if (strcmp(lineHeader, "vn") == 0) {
            glm::vec3 normal;
            fscanf(file, "%f %f %f\n", &normal.x, &normal.y, &normal.z);
            mesh->normals.push_back(normal);
        }
        else if (strcmp(lineHeader, "f") == 0) {
            std::string vertex1, vertex2, vertex3;
            unsigned int vertexIndex[3], uvIndex[3], normalIndex[3];
            int matches = fscanf(file, "%d/%d/%d %d/%d/%d %d/%d/%d\n", 
                &vertexIndex[0], &uvIndex[0], &normalIndex[0], 
                &vertexIndex[1], &uvIndex[1], &normalIndex[1], 
                &vertexIndex[2], &uvIndex[2], &normalIndex[2]);
            if (matches < 9)
                continue;

            mesh->indices.push_back(CCFace(vertexIndex[0] - 1, normalIndex[0] - 1,
                    uvIndex[0] - 1));
            mesh->indices.push_back(CCFace(vertexIndex[1] - 1, normalIndex[1] - 1,
                    uvIndex[1] - 1));
            mesh->indices.push_back(CCFace(vertexIndex[2] - 1, normalIndex[2] - 1,
                    uvIndex[2] - 1));
        }
        else {
            // Probably a comment, eat up the rest of the line  
            char stupidBuffer[1000];
            fgets(stupidBuffer, 1000, file);
        }
    }

    fclose(file);

    LOGIF(LOG_TAG, " Load obj %s", path);
    LOGIF(LOG_TAG, "vertex count: %d , normals  count: %d , texCoords count: %d , indices count: %d",
            mesh->positions.size(), mesh->normals.size(), mesh->texCoords.size(), mesh->indices.size());

    mesh->GenerateBuffer();
    return true;
}
bool CCObjLoader::Load(BYTE *buffer, size_t bufferSize, CCMesh *mesh) {
    if(buffer && mesh) {

        const char* string = (char*)buffer;
        const char* strPos = nullptr;
        ULONG off = 0, strSize = (ULONG)strPos - (ULONG)string;
        while (true) {

            char lineHeader[128];
            strPos = strchr(string, '\n');
            if(strPos == nullptr)
                break;

            strncpy(lineHeader, (char*)((ULONG)string + off), strSize);
            off += strSize;

            if (strncmp(lineHeader, "v", 1) == 0) {
                glm::vec3 vertex;
                sscanf(lineHeader, "v %f %f %f", &vertex.x, &vertex.y, &vertex.z);
                mesh->positions.push_back(vertex);
            }
            else if (strncmp(lineHeader, "vt", 2) == 0) {
                glm::vec2 uv;
                sscanf(lineHeader, "vt %f %f", &uv.x, &uv.y);
                mesh->texCoords.push_back(uv);
            }
            else if (strncmp(lineHeader, "vn", 2) == 0) {
                glm::vec3 normal;
                sscanf(lineHeader, "vn %f %f %f", &normal.x, &normal.y, &normal.z);
                mesh->normals.push_back(normal);
            }
            else if (strncmp(lineHeader, "f", 1) == 0) {
                std::string vertex1, vertex2, vertex3;
                unsigned int vertexIndex[3], uvIndex[3], normalIndex[3];
                int matches = sscanf(lineHeader, "f %d/%d/%d %d/%d/%d %d/%d/%d",
                                     &vertexIndex[0], &uvIndex[0], &normalIndex[0],
                                     &vertexIndex[1], &uvIndex[1], &normalIndex[1],
                                     &vertexIndex[2], &uvIndex[2], &normalIndex[2]);
                if (matches < 9)
                    continue;

                mesh->indices.push_back(CCFace(vertexIndex[0] - 1, normalIndex[0] - 1,
                                               uvIndex[0] - 1));
                mesh->indices.push_back(CCFace(vertexIndex[1] - 1, normalIndex[1] - 1,
                                               uvIndex[1] - 1));
                mesh->indices.push_back(CCFace(vertexIndex[2] - 1, normalIndex[2] - 1,
                                               uvIndex[2] - 1));
            }
        }

        LOGIF(LOG_TAG, " Load obj 0x%x, size : %d", buffer, bufferSize);
        LOGIF(LOG_TAG, "vertex count: %d , normals  count: %d , texCoords count: %d , indices count: %d",
              mesh->positions.size(), mesh->normals.size(), mesh->texCoords.size(), mesh->indices.size());

        mesh->GenerateBuffer();
        return true;
    }
    return false;
}
