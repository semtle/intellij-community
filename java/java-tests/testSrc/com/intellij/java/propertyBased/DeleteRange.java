/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.java.propertyBased;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import slowCheck.Generator;
import slowCheck.IntDistribution;

import java.util.Objects;

class DeleteRange extends ActionOnRange implements MadTestingAction {
  private final PsiFile myFile;

  private DeleteRange(PsiFile file, int startOffset, int endOffset) {
    super(file.getViewProvider().getDocument(), startOffset, endOffset);
    assert myMarker.getDocument().getTextLength() == file.getTextLength();
    myFile = file;
  }

  static Generator<DeleteRange> deletePsiRange(@NotNull PsiFile psiFile) {
    return Generator.from(data -> {
      if (psiFile.getTextLength() == 0) return new DeleteRange(psiFile, 0, 0);

      int startOffset = Generator.integers(0, psiFile.getTextLength() - 1).generateValue(data);
      PsiElement start = psiFile.findElementAt(startOffset);
      PsiElement end = psiFile.findElementAt(startOffset + data.drawInt(IntDistribution.geometric(10)));
      if (start == null || end == null) return null;

      PsiElement commonParent = PsiTreeUtil.findCommonParent(start, end);
      return new DeleteRange(psiFile,
                             commonParent.getTextRange().getStartOffset(),
                             commonParent.getTextRange().getEndOffset());
    }).suchThat(Objects::nonNull);
  }

  @Override
  public String toString() {
    return "DeleteRange: " + myFile.getVirtualFile().getPath() + " " + getCurrentRange();
  }

  public void performAction() {
    TextRange range = getFinalRange();
    if (range == null) return;

    WriteCommandAction.runWriteCommandAction(myFile.getProject(), () -> myFile.getViewProvider().getDocument()
      .deleteString(range.getStartOffset(), range.getEndOffset()));

    PsiUtilCore.ensureValid(myFile);
    PsiTestUtil.checkStubsMatchText(myFile);
  }
}
