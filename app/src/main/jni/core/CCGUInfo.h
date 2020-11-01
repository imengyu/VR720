#pragma once
#include "stdafx.h"
#include "CImageLoader.h"

class CCGUInfo
{
public:
	bool currentImageOpened = false;

	std::string currentImageName;
	std::string currentImageSize;
	std::string currentImageImgSize;
	std::string currentImageChangeDate;

	ImageType currentImageType;

	int currentImageAllChunks = 0;
	int currentImageLoadChunks = 0;
	int currentImageLoadedChunks = 0;
	bool currentImageLoading = false;
};

