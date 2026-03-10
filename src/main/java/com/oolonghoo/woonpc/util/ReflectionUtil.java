package com.oolonghoo.woonpc.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具类
 * 提供缓存机制优化反射性能
 * 
 * @author oolonghoo
 */
public final class ReflectionUtil {
    
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static final Map<String, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    
    private ReflectionUtil() {
    }
    
    // ==================== 类加载 ====================
    
    /**
     * 获取类（带缓存）
     * 
     * @param className 完整类名
     * @return 类对象，不存在返回null
     */
    public static Class<?> getClass(String className) {
        return classCache.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().warning("[WooNPC] Class not found: " + name);
                return null;
            }
        });
    }
    
    /**
     * 获取 NMS 类
     * 
     * @param nmsClassName NMS 类名（不含包前缀）
     * @return 类对象
     */
    public static Class<?> getNmsClass(String nmsClassName) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        return getClass("net.minecraft.server." + version + "." + nmsClassName);
    }
    
    /**
     * 获取 CraftBukkit 类
     * 
     * @param craftClassName CraftBukkit 类名
     * @return 类对象
     */
    public static Class<?> getCraftClass(String craftClassName) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        return getClass("org.bukkit.craftbukkit." + version + "." + craftClassName);
    }
    
    // ==================== 方法反射 ====================
    
    /**
     * 获取方法（带缓存）
     * 
     * @param clazz 类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法对象，不存在返回null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        String cacheKey = clazz.getName() + "." + methodName + "(" + getParameterTypesKey(parameterTypes) + ")";
        
        return methodCache.computeIfAbsent(cacheKey, key -> {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                // 尝试查找父类
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null) {
                    return getMethod(superClass, methodName, parameterTypes);
                }
                Bukkit.getLogger().warning("[WooNPC] Method not found: " + key);
                return null;
            }
        });
    }
    
    /**
     * 调用方法
     * 
     * @param instance 实例对象
     * @param methodName 方法名
     * @param args 参数
     * @return 返回值
     */
    public static Object invokeMethod(Object instance, String methodName, Object... args) {
        if (instance == null) return null;
        
        Class<?>[] parameterTypes = getParameterTypes(args);
        Method method = getMethod(instance.getClass(), methodName, parameterTypes);
        
        if (method == null) return null;
        
        try {
            return method.invoke(instance, args);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to invoke method: " + methodName + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 调用静态方法
     * 
     * @param clazz 类
     * @param methodName 方法名
     * @param args 参数
     * @return 返回值
     */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) return null;
        
        Class<?>[] parameterTypes = getParameterTypes(args);
        Method method = getMethod(clazz, methodName, parameterTypes);
        
        if (method == null) return null;
        
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to invoke static method: " + methodName + " - " + e.getMessage());
            return null;
        }
    }
    
    // ==================== 字段反射 ====================
    
    /**
     * 获取字段（带缓存）
     * 
     * @param clazz 类
     * @param fieldName 字段名
     * @return 字段对象，不存在返回null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "." + fieldName;
        
        return fieldCache.computeIfAbsent(cacheKey, key -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // 尝试查找父类
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null) {
                    return getField(superClass, fieldName);
                }
                Bukkit.getLogger().warning("[WooNPC] Field not found: " + key);
                return null;
            }
        });
    }
    
    /**
     * 获取字段值
     * 
     * @param instance 实例对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Object getFieldValue(Object instance, String fieldName) {
        if (instance == null) return null;
        
        Field field = getField(instance.getClass(), fieldName);
        if (field == null) return null;
        
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to get field value: " + fieldName + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置字段值
     * 
     * @param instance 实例对象
     * @param fieldName 字段名
     * @param value 新值
     * @return 是否成功
     */
    public static boolean setFieldValue(Object instance, String fieldName, Object value) {
        if (instance == null) return false;
        
        Field field = getField(instance.getClass(), fieldName);
        if (field == null) return false;
        
        try {
            field.set(instance, value);
            return true;
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to set field value: " + fieldName + " - " + e.getMessage());
            return false;
        }
    }
    
    // ==================== 构造器反射 ====================
    
    /**
     * 获取构造器（带缓存）
     * 
     * @param clazz 类
     * @param parameterTypes 参数类型
     * @return 构造器对象，不存在返回null
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        String cacheKey = clazz.getName() + ".<init>(" + getParameterTypesKey(parameterTypes) + ")";
        
        return constructorCache.computeIfAbsent(cacheKey, key -> {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                Bukkit.getLogger().warning("[WooNPC] Constructor not found: " + key);
                return null;
            }
        });
    }
    
    /**
     * 创建实例
     * 
     * @param clazz 类
     * @param args 参数
     * @return 实例对象
     */
    public static Object newInstance(Class<?> clazz, Object... args) {
        if (clazz == null) return null;
        
        Class<?>[] parameterTypes = getParameterTypes(args);
        Constructor<?> constructor = getConstructor(clazz, parameterTypes);
        
        if (constructor == null) return null;
        
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[WooNPC] Failed to create instance: " + clazz.getName() + " - " + e.getMessage());
            return null;
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private static Class<?>[] getParameterTypes(Object... args) {
        if (args == null || args.length == 0) {
            return new Class<?>[0];
        }
        
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                types[i] = Object.class;
            } else {
                types[i] = args[i].getClass();
            }
        }
        return types;
    }
    
    private static String getParameterTypesKey(Class<?>... parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(parameterTypes[i] != null ? parameterTypes[i].getName() : "null");
        }
        return sb.toString();
    }
    
    /**
     * 清除所有缓存
     */
    public static void clearCache() {
        methodCache.clear();
        fieldCache.clear();
        constructorCache.clear();
        classCache.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计字符串
     */
    public static String getCacheStats() {
        return String.format("ReflectionUtil Cache: classes=%d, methods=%d, fields=%d, constructors=%d",
                classCache.size(), methodCache.size(), fieldCache.size(), constructorCache.size());
    }
}
