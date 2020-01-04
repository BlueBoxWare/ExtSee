package com.gmail.blueboxware.extsee.java

import com.gmail.blueboxware.extsee.ExtSeeExtensionTreeElement
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.structureView.impl.java.PsiMethodTreeElement
import com.intellij.ide.structureView.impl.java.SuperTypeGroup
import com.intellij.ide.structureView.impl.java.SuperTypesGrouper
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.ProjectAndLibrariesScope
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.checker.isClassType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import java.lang.ref.WeakReference


/*
 * Copyright 2017 Blue Box Ware
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
internal class ExtSeeSuperTypesGrouper: Grouper {

  override fun group(parent: AbstractTreeNode<*>, children: MutableCollection<TreeElement>): Collection<Group> {

    if (isParentGrouped(parent)) {
      return listOf()
    }

    val project = parent.project ?: return listOf()

    val psiFacade = JavaPsiFacade.getInstance(project)
    val scope = ProjectAndLibrariesScope(project, true)

    val groups = mutableMapOf<Group, SuperTypeGroup>()

    for (child in children) {

      if (child is ExtSeeExtensionTreeElement) {
        if (child.isInHerited) {
          ((child.callableDeclaration.descriptor as? CallableDescriptor)?.extensionReceiverParameter?.type as? SimpleType)?.let { simpleType ->
            var ownerType: SimpleType? = simpleType
            if (ownerType?.isClassType != true) {
              ownerType = ownerType?.immediateSupertypes()?.firstOrNull { (it as? SimpleType)?.isClassType == true } as? SimpleType
            }
            if (ownerType?.isClassType == true) {
              DescriptorUtils.getClassDescriptorForType(ownerType).classId?.asSingleFqName()?.asString()?.let {
                var classFqName = it
                if (classFqName == "kotlin.Any") {
                  classFqName = "java.lang.Object"
                }
                psiFacade.findClass(classFqName, scope)?.let {
                  val group = getOrCreateGroup(it, SuperTypeGroup.OwnershipType.INHERITS, groups)
                  group.addMethod(child)
                }
              }
            }
          }
        }
      } else if (child is PsiMethodTreeElement) {
        val method = child.method ?: continue
        if (child.isInherited) {
          method.containingClass?.let { containingClass ->
            val group = getOrCreateGroup(containingClass, SuperTypeGroup.OwnershipType.INHERITS, groups)
            group.addMethod(child)
          }
        } else {
          val superMethods = method.findSuperMethods()

          if (superMethods.isNotEmpty()) {

            for (i in 1 until superMethods.size) {
              val superMethod = superMethods.firstOrNull()
              val containingClass = superMethod?.containingClass
              if (containingClass?.isInterface == true) {
                ArrayUtil.swap(superMethods, 0, i)
              }
            }

            val superMethod = superMethods.firstOrNull() ?: continue
            method.putUserData(SUPER_METHOD_KEY, WeakReference(superMethod))
            superMethod.containingClass?.let { containingClass ->
              val overrides = methodOverridesSuper(method, superMethod)
              val ownerShipType = if (overrides) SuperTypeGroup.OwnershipType.OVERRIDES else SuperTypeGroup.OwnershipType.IMPLEMENTS
              val group = getOrCreateGroup(containingClass, ownerShipType, groups)
              group.addMethod(child)
            }
          }
        }
      }

    }

    return groups.keys

  }

  override fun getPresentation(): ActionPresentation =
          ActionPresentationData(
                  IdeBundle.message("action.structureview.group.methods.by.defining.type"),
                  null,
                  AllIcons.General.ImplementingMethod
          )

  override fun getName(): String = SuperTypesGrouper.ID

  companion object {

    private val SUPER_METHOD_KEY = Key.create<WeakReference<PsiMethod>>("StructureTreeBuilder.SUPER_METHOD_KEY")

    private fun getOrCreateGroup(parent: PsiClass, ownershipType: SuperTypeGroup.OwnershipType, groups: MutableMap<Group, SuperTypeGroup>): SuperTypeGroup {

      val superTypeGroup = SuperTypeGroup(parent, ownershipType)
      var existing = groups[superTypeGroup]

      if (existing == null) {
        groups[superTypeGroup] = superTypeGroup
        existing = superTypeGroup
      }

      return existing

    }

    private fun isParentGrouped(parentNode: AbstractTreeNode<*>): Boolean {

      var parent: AbstractTreeNode<*>? = parentNode

      while (parent != null) {
        if (parent.value is SuperTypeGroup) {
          return true
        }
        parent = parent.parent
      }

      return false
    }

    private fun methodOverridesSuper(method: PsiMethod, superMethod: PsiMethod): Boolean =
            method.hasModifierProperty(PsiModifier.ABSTRACT) || !superMethod.hasModifierProperty(PsiModifier.ABSTRACT)

  }

}