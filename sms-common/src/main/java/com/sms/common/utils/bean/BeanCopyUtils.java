package com.sms.common.utils.bean;

import com.github.pagehelper.Page;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entity 與 DTO/VO 之間的屬性拷貝工具
 */
public final class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    public static <S, T> T copy(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Copy failed: " + source.getClass() + " -> " + targetClass, e);
        }
    }

    public static <S, T> T copy(S source, T target) {
        if (source == null || target == null) {
            return target;
        }
        BeanUtils.copyProperties(source, target);
        return target;
    }

    public static <S, T> List<T> copyList(List<S> sources, Class<T> targetClass) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(sources.size());
        for (S source : sources) {
            result.add(copy(source, targetClass));
        }
        return result;
    }

    /**
     * 分頁列表拷貝：保留 PageHelper 的 total 等分頁元數據，供 getDataTable 正確返回總數。
     */
    public static <S, T> List<T> copyPageList(List<S> sources, Class<T> targetClass) {
        if (sources == null) {
            return Collections.emptyList();
        }
        if (!(sources instanceof Page)) {
            return copyList(sources, targetClass);
        }
        Page<S> sourcePage = (Page<S>) sources;
        Page<T> targetPage = new Page<>(sourcePage.getPageNum(), sourcePage.getPageSize());
        targetPage.setTotal(sourcePage.getTotal());
        targetPage.setPages(sourcePage.getPages());
        for (S source : sources) {
            targetPage.add(copy(source, targetClass));
        }
        return targetPage;
    }

    public static <S, T> List<T> copyList(List<S> sources, Function<S, T> converter) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        return sources.stream().map(converter).collect(Collectors.toList());
    }

    /**
     * 樹形結構拷貝：遞歸將節點及其 children 轉為目標類型。
     */
    public static <S, T> List<T> copyTree(List<S> nodes, Class<T> targetClass,
            Function<S, List<S>> childrenGetter, BiConsumer<T, List<T>> childrenSetter) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(nodes.size());
        for (S node : nodes) {
            result.add(copyTreeNode(node, targetClass, childrenGetter, childrenSetter));
        }
        return result;
    }

    private static <S, T> T copyTreeNode(S node, Class<T> targetClass,
            Function<S, List<S>> childrenGetter, BiConsumer<T, List<T>> childrenSetter) {
        T target = copy(node, targetClass);
        List<S> children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
            childrenSetter.accept(target, copyTree(children, targetClass, childrenGetter, childrenSetter));
        }
        return target;
    }
}
