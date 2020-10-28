#include "CCObjLoader.h"
#include "CApp.h"
#include "CCMesh.h"
#include <vector>

bool CCObjLoader::Load(const wchar_t* path, CCMesh* mesh)
{
    FILE* file;
    _wfopen_s(&file, path, L"r");
    if (file == NULL) {
        CApp::Instance->GetLogger()->LogError2(L"[CCObjLoader] Open %s failed !", path);
        return false;
    }

    while (1) {

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

            mesh->indices.push_back(CCFace(vertexIndex[0] - 1, normalIndex[0] - 1, uvIndex[0] - 1));
            mesh->indices.push_back(CCFace(vertexIndex[1] - 1, normalIndex[1] - 1, uvIndex[1] - 1));
            mesh->indices.push_back(CCFace(vertexIndex[2] - 1, normalIndex[2] - 1, uvIndex[2] - 1));
        }
        else {
            // Probably a comment, eat up the rest of the line  
            char stupidBuffer[1000];
            fgets(stupidBuffer, 1000, file);
        }
    }
    fclose(file);

    Logger* logger = CApp::Instance->GetLogger();
    logger->Log(L"[CCObjLoader]  Load obj %s", path);
    logger->Log(L"vertex count: %d , normals  count: %d , texCoords count: %d , indices count: %d", mesh->positions.size(), mesh->normals.size(), mesh->texCoords.size(), mesh->indices.size());

    mesh->GenerateBuffer();
    return true;
}
