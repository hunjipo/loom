/*
* Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
* or visit www.oracle.com if you need additional information or have any
* questions.
*/

package java.lang;

import java.lang.StackWalker.StackFrame;
import java.lang.StackWalker.Option;
import java.lang.LiveStackFrame.PrimitiveSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class StackWalkerHelper {
    private static final Set<Option> OPTS = EnumSet.of(Option.SHOW_REFLECT_FRAMES); // EnumSet.noneOf(Option.class);

    public static StackFrame[] getStackFrames(ContinuationScope scope)     { return getStackFrames(StackWalker.getInstance(OPTS, scope)); }
    public static StackFrame[] getStackFrames(Continuation cont)           { return getStackFrames(cont.stackWalker(OPTS)); }

    public static StackFrame[] getLiveStackFrames(ContinuationScope scope) { return getStackFrames(LiveStackFrame.getStackWalker(OPTS, scope)); }
    public static StackFrame[] getLiveStackFrames(Continuation cont)       { return getStackFrames(LiveStackFrame.getStackWalker(OPTS, cont.getScope(), cont)); }

    public static StackFrame[] getStackFrames(StackWalker walker) {
        return walker.walk(fs -> fs.collect(Collectors.toList())).toArray(new StackFrame[0]);
    }

    public static StackTraceElement[] toStackTraceElement(StackFrame[] fs) {
        StackTraceElement[] out = new StackTraceElement[fs.length];
        for (int i = 0; i < fs.length; i++) {
            out[i] = fs[i].toStackTraceElement();
        }
        return out;
    }

    public static boolean equals(StackFrame[] a, StackFrame[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (!equals(a[i], b[i])) return false;
        return true;
    }

    public static boolean equals(StackFrame a, StackFrame b) {
        if ( !(Objects.equals(a.getClassName(),     b.getClassName())
            && Objects.equals(a.getMethodName(),    b.getMethodName())
            // && Objects.equals(a.getByteCodeIndex(), b.getByteCodeIndex()) // TODO !!!!
            && Objects.equals(a.getContinuationScopeName(), b.getContinuationScopeName())
            )) {
            // System.out.println("XXXX\ta: " + a + " a.scope: " + a.getContinuationScopeName() + " a.bci: " + a.getByteCodeIndex() + " a.toSTE: " + a.toStackTraceElement()
            //                    + "\n\tb: " + b + " b.scope: " + b.getContinuationScopeName() + " b.bci: " + b.getByteCodeIndex() + " b.toSTE: " + b.toStackTraceElement());
            return false;
        }
        try {
            if ( !(Objects.equals(a.getDeclaringClass(), b.getDeclaringClass())
                && Objects.equals(a.getMethodType(),     b.getMethodType()))) {
            return false;
        }
        } catch(UnsupportedOperationException e) {}
        // assert Objects.equals(a.toStackTraceElement(), b.toStackTraceElement()) : "a" + a.toStackTraceElement() + " b: " + b.toStackTraceElement();
        if (!Objects.equals(a.toStackTraceElement(), b.toStackTraceElement()))
            return false;

        if (!(a instanceof LiveStackFrame && b instanceof LiveStackFrame))
            return true;

        LiveStackFrame la = (LiveStackFrame)a;
        LiveStackFrame lb = (LiveStackFrame)b;

        if (!Objects.equals(la.getMonitors(), lb.getMonitors())) return false;
        if (!slotEquals(la.getLocals(), lb.getLocals())) return false;
        if (!slotEquals(la.getStack(),  lb.getStack()))  return false;
        return true;
    }

    private static boolean slotEquals(Object a, Object b) {
        if (!(a instanceof PrimitiveSlot || b instanceof PrimitiveSlot))
            return Objects.equals(a, b);

        if (!(a instanceof PrimitiveSlot && b instanceof PrimitiveSlot))
            return false;
        PrimitiveSlot pa = (PrimitiveSlot)a;
        PrimitiveSlot pb = (PrimitiveSlot)b;

        return pa.size() == pb.size() && switch(pa.size()) {
            case 4 -> pa.intValue()  == pb.intValue();
            case 8 -> pa.longValue() == pb.longValue();
            default -> throw new AssertionError("Slot size is " + pa.size());
        };
    }
}