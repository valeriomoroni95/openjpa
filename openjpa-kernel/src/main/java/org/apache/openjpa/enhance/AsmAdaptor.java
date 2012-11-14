/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.enhance;

import serp.bytecode.BCClass;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Use ASM to add required StackMapTable attribute to the byte code generated by
 * Serp.
 *
 * This class contains a small hack to pickup different known shades of ASM
 * to prevent classpath clashes.
 * We first try to use standard ASM. If this is not available we try to pickup
 * the shaded xbean-asm version used in OpenEJB, Geronimo or WAS.
 * At last we try to use the shaded version from Spring.
 */
public final class AsmAdaptor {
    private static final Localizer _loc = Localizer.forPackage(AsmAdaptor.class);
    
    private static final int Java7_MajorVersion = 51;

    private static Class<?> cwClass;
    private static Class<?> crClass;
    private static int COMPUTE_FRAMES;
    private static Method classReaderAccept;
    private static Method classWritertoByteArray;
    private static Constructor<?> classWriterConstructor;
    private static Constructor<?> classReaderConstructor;

    static {
        // try the "real" asm first, then the others
        tryClass("org.objectweb.asm.");
        tryClass("org.apache.xbean.asm.");
        tryClass("org.springframework.asm.");

        // get needed stuff
        try {
            COMPUTE_FRAMES = cwClass.getField("COMPUTE_FRAMES").getInt(null);
            classReaderAccept = crClass.getMethod("accept", cwClass.getInterfaces()[0], int.class);
            classReaderConstructor = crClass.getConstructor(InputStream.class);
            classWriterConstructor = cwClass.getConstructor(int.class);
            classWritertoByteArray = cwClass.getMethod("toByteArray");
        } catch (Exception e) {
            throw new IllegalStateException(_loc.get("static-asm-exception").getMessage(), e);
        }
    }

    private static void tryClass(final String s) {
        if (cwClass == null) {
            try {
                cwClass = AsmAdaptor.class.getClassLoader().loadClass(s + "ClassWriter");
            } catch (Throwable t) {
                //ignore
            }
        }
        if (crClass == null) {
            try {
                crClass = AsmAdaptor.class.getClassLoader().loadClass(s + "ClassReader");
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void write(BCClass bc) throws IOException {
        if (bc.getMajorVersion() < Java7_MajorVersion) {
            bc.write();
        } else {
            String name = bc.getName();
            int dotIndex = name.lastIndexOf('.') + 1;
            name = name.substring(dotIndex);
            Class<?> type = bc.getType();

            OutputStream out = new FileOutputStream(
                    URLDecoder.decode(type.getResource(name + ".class").getFile()));
            try {
                writeJava7(bc, out);
            } finally {
                out.flush();
                out.close();
            }
        }
    }

    public static void write(BCClass bc, File outFile) throws IOException {
        if (bc.getMajorVersion() < Java7_MajorVersion) {
            bc.write(outFile);
        } else {
            OutputStream out = new FileOutputStream(outFile);
            try {
                writeJava7(bc, out);
            } finally {
                out.flush();
                out.close();
            }
        }
    }

    public static byte[] toByteArray(BCClass bc, byte[] returnBytes) throws IOException {
        if (bc.getMajorVersion() >= Java7_MajorVersion) {
            returnBytes = toJava7ByteArray(bc, returnBytes);
        }
        return returnBytes;
    }

    private static void writeJava7(BCClass bc, OutputStream out) throws IOException {
        byte[] java7Bytes = toJava7ByteArray(bc, bc.toByteArray());
        out.write(java7Bytes);
    }

    private static byte[] toJava7ByteArray(final BCClass bc, final byte[] classBytes) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(classBytes);
        final BufferedInputStream bis = new BufferedInputStream(bais);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            final Object cw = classWriterConstructor.newInstance(COMPUTE_FRAMES);
            final Object cr = classReaderConstructor.newInstance(bis);

            // ClassWriter.getCommonSuperClass uses TCCL
            Thread.currentThread().setContextClassLoader(bc.getClassLoader());
            classReaderAccept.invoke(cr, cw, 0);

            return (byte[]) classWritertoByteArray.invoke(cw);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}