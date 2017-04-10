import React from 'react'
import R from 'ramda'
import {contextualizeModel, addContext, contextualizeSubModel, modelLookup} from './EditorModel'

/*
    model: required model object
    path: optional path to actually used model. It's ok if the model cannot be found by path: a NullEditor will be used
 */
export const Editor = ({ model, editorMapping, changeBus, doneEditingBus, path, edit, ...rest }) => {
  if (!model) {
    throw new Error('model missing')
  }
  if (!model.context) {
    if (!editorMapping) {
      console.error('editorMapping required for root editor', model)
      throw new Error('editorMapping required for root editor')
    }
    R.toPairs(editorMapping).forEach(([key, value]) => { if (!value) throw new Error('Editor missing for ' + key) })
    model = contextualizeModel(model, {
      changeBus, doneEditingBus,
      root: true,
      path: '',
      prototypes: model.prototypes,
      editorMapping
    })
  }
  edit = parseBool(edit)
    if (edit !== model.context.edit) {
    model = addContext(model, { edit })
  }
  if (path) {
    model = modelLookup(model, path)
  }
  return getModelEditor(model, rest)
}

const parseBool = (b) => {
  if (typeof b === 'string') {
    return b === 'true'
  }
  return b
}
Editor.propTypes = {
  model: React.PropTypes.object.isRequired
}
Editor.canShowInline = (model) => (getEditorFunction(model).canShowInline || (() => false))(model)
Editor.handlesOptional = (model) => getEditorFunction(model).handlesOptional

const NullEditor = React.createClass({
  render() {
    return null
  }
})

const getEditorFunction = (model) => {
  let editorByClass = filter => mdl => {
    if (!mdl || !mdl.value) {
      return undefined
    }
    for (var i in mdl.value.classes) {
      var editor = mdl.context.editorMapping[mdl.value.classes[i]]
      if (editor && (!mdl.context.edit || filter(editor))) { return editor }
    }
  }

  let notReadOnlyEditor = editorByClass(e => !e.readOnly)
  let optionalHandlingEditor = editorByClass(e => e.handlesOptional)

  if (!model) return NullEditor

  if (model.optional) {
    let prototype = model.optionalPrototype && contextualizeSubModel(model.optionalPrototype, model)
    let typeEditor = prototype && model.context.editorMapping[prototype.type]
    return optionalHandlingEditor(prototype) || (typeEditor && typeEditor.handlesOptional && typeEditor) || model.context.editorMapping.optional
  }

  let editor = notReadOnlyEditor(model) || model.context.editorMapping[model.type]
  if (!editor) {
    if (!model.type) {
      console.error('Typeless model', model)
    } else {
      console.error('Missing editor for type "' + model.type + '"')
    }
    return NullEditor
  }
  return editor
}

const getModelEditor = (model, props) => {
  if (model && !model.context) {
    console.error('Context missing from model', model)
  }
  var ModelEditor = getEditorFunction(model)
  return <ModelEditor model={model} {...props} />
}