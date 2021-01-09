function linkClickEvent(e) {
  var url = e.target.getAttribute('data-open-page');
  if(url != '')
    jsi.openPage(url);
}
window.onload = function() {
  var list = document.querySelectorAll('a[data-open-page],div[data-open-page]');
  list.forEach(function(v) { 
    v.onclick = linkClickEvent; 
  });
  list = document.querySelectorAll('.hidden-content');
  var show = jsi.getIsGithubBuild();
  list.forEach(function (v) {
    v.setAttribute('style', show ? '' : 'display:none');
  });

  var lang = jsi.getLanguage();
  if (lang == '' || lang == 'zh')  {
    list = document.querySelectorAll('.content-translate[data-language="zh"]');
    list.forEach(function(v) { 
      v.setAttribute('style', 'display:block');
    });
    var div = document.querySelector('.content-translate[data-language="zh"]');
    if(div != null) {
      var title = div.getAttribute('data-title');
      if(title != null && title != '') document.title = title;
    }
  } else {
    list = document.querySelectorAll('.content-translate[data-language="en"]');
    list.forEach(function(v) { 
      v.setAttribute('style', 'display:block');
    });
    var div = document.querySelector('.content-translate[data-language="en"]');
    if(div != null) {
      var title = div.getAttribute('data-title');
      if(title != null && title != '') document.title = title;
    }
  }
};