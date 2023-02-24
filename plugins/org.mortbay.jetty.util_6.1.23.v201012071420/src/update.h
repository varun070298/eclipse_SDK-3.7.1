/* DO NOT EDIT THIS FILE - it is machine generated */

/**********************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 **********************************************************************/
#include <jni.h>
/* Header for class org_eclipse_update_configuration_LocalSystemInfo */

#ifndef _Included_org_eclipse_update_configuration_LocalSystemInfo
#define _Included_org_eclipse_update_configuration_LocalSystemInfo
#ifdef __cplusplus
extern "C" {
#endif
#undef org_eclipse_update_configuration_LocalSystemInfo_SIZE_UNKNOWN
#define org_eclipse_update_configuration_LocalSystemInfo_SIZE_UNKNOWN -1L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_UNKNOWN
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_UNKNOWN -1L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_INVALID_PATH
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_INVALID_PATH -2L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOVABLE
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOVABLE 1L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FIXED
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FIXED 2L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOTE
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOTE 3L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_CDROM
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_CDROM 4L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_RAMDISK
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_RAMDISK 5L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_5
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_5 6L
#undef org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_3
#define org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_3 7L
/* Inaccessible static: listeners */
/* Inaccessible static: hasNatives */
/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetFreeSpace
 * Signature: (Ljava/io/File;)J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetFreeSpace
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetLabel
 * Signature: (Ljava/io/File;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetLabel
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetType
 * Signature: (Ljava/io/File;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetType
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeListMountPoints
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeListMountPoints
  (JNIEnv *, jclass);

#ifdef __cplusplus
}

#endif
#endif
