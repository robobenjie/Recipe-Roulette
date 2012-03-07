
window.display_search = ->
  $.post "/waiting", {"search-string": recipe_title_string}, (result) ->
    console.log result
    $('#main-content-div').html result
    setTimeout ->
      display_search()
    , 500
    setTimeout ->
      window.location.href = recipe_title_string
    , 8000