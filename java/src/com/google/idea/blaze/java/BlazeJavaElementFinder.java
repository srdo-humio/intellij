package com.google.idea.blaze.java;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder;

/**
 * Find classes while favoring the actual location of the class file to the duplicates found in
 * META-INF/TRANSITIVE of other libraries.
 */
public final class BlazeJavaElementFinder extends PsiElementFinder {

  private BlazeJavaElementFinder() {}

  public static BlazeJavaElementFinder getInstance(Project project) {
    return (BlazeJavaElementFinder)
        PsiElementFinder.EP
            .getPoint(project)
            .extensions()
            .filter(e -> e instanceof BlazeJavaElementFinder)
            .findFirst()
            .get();
  }

  @Override
  public @Nullable PsiClass findClass(
      @NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    PsiClass[] classes =
        JavaElementFinder.Companion.getInstance(scope.getProject())
            .findClasses(qualifiedName, scope);
    if (classes.length == 0) {
      return null;
    }

    Optional<PsiClass> cls =
        Arrays.stream(classes)
            .filter(
                c ->
                    !c.getContainingFile()
                        .getContainingDirectory()
                        .getVirtualFile()
                        .getPath()
                        .contains("/META-INF/TRANSITIVE"))
            .findFirst();
    if (cls.isEmpty()) {
      return classes[0];
    }
    return cls.get();
  }

  @Override
  public @NotNull PsiClass[] findClasses(
      @NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    return JavaElementFinder.Companion.getInstance(scope.getProject())
        .findClasses(qualifiedName, scope);
  }
}
