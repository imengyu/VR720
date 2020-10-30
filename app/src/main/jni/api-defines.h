#pragma once
#ifndef API_DEFINES_H
#define API_DEFINES_H

//string api

#if WCHAR_API

#if defined(_MSC_VER) && _MSC_VER > 1600
#define _v_fprintf fwprintf_s
#define _v_printf wprintf_s
#define _v_fopen_s _wfopen_s
#else
#define _v_fprintf fwprintf
#define _v_printf wprintf
#define _v_fopen_s _wfopen
#endif

#define _v_fopen _wfopen

#else

#define _v_fprintf fprintf
#define _v_printf printf
#define _v_fopen_s fopen_s
#define _v_fopen fopen

#endif

//others api

#if defined(_MSC_VER) && _MSC_VER > 1600

#else

#endif

#endif // !API_DEFINES_H

