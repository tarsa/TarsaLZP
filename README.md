- License: New BSD License
- Copyright (c) 2012, Piotr Tarsa

##Project description

This project contains implementations of a single compression algorithm in different languages and performance measurements of those implementations under available execution environments.

##Project goal

The goal is to evaluate programming platforms usefullness when it comes to develop algorithms that are heavy on computations. Ideally, a programming platform should have comparatively low resource usage and execute programs quickly while hiding complexities related to the programming platform and allowing the programmer to focus only on the problem that he was originally trying to solve. Usually, comparisons between execution environments for different programming languages are done using microbenchmarks. Unfortunately those microbenchmarks are too simple to draw any sensible conclusion. This project provides several implementations for a single nontrivial problem and focuses on side effects (ie produced output) while providing freedom to implementation details.

The requirements for any implementation are:

 - Each implementation must produce identical output when fed with identical input and compression options
 - Each implementation should expose all compression options that the format defines and allow for adjustment of that options at encoding phase
 - Languages mixing is not allowed, eg mixing Python code with non-standard C modules
 - There must be no side effects other than the output, ie the decompressed stream when decompressing, compressed stream when compressing and compression options when checking them

Parallelization is allowed.

##Performance results

All tests are performed on following system:

- CPU: Intel Core 2 Duo E8400 @ 3.00 GHz (stock),
- RAM: 8 GiB A-Data DDR3 RAM @ CL5, 1000 MHz
- OS: Ubuntu 12.04 64-bit

Encoding speed on [enwik8] [1] (command line interface implementations) - revision [b69b99d] [2]:
<table>
  <thead>
    <tr>
      <th>Language</th>
      <th>Real time</th>
      <th>User time</th>
      <th>Sys time</th>
      <th>Execution environment</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>C</td>
      <td>7.876s</td>
      <td>7.607s</td>
      <td>0.240s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, SSE optimizations (vectors and prefetching)</td>
    </tr>
    <tr>
      <td>C</td>
      <td>12.922s</td>
      <td>12.653s</td>
      <td>0.235s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, no SSE optimizations</td>
    </tr>
    <tr>
      <td>Java</td>
      <td>21.999s</td>
      <td>21.786s</td>
      <td>0.323s</td>
      <td>Oracle JDK 7 update 9</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>151.667s</td>
      <td>144.684s</td>
      <td>6.363s</td>
      <td>PyPy 1.9.0</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>1752.591s</td>
      <td>1751.693s</td>
      <td>0.424s</td>
      <td>CPython 2.7.3</td>
      <td>Single run, -OO</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik8] [1] (JavaScript in browsers) - revision [b69b99d] [2]:
<table>
  <thead>
    <tr>
      <th>Browser</th>
      <th>Reported time</th>
      <th>Browser version</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Chrome</td>
      <td>54.770s</td>
      <td>23.0.1271.95</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Opera</td>
      <td>144.922s</td>
      <td>12.11 (build 1661)</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Firefox</td>
      <td>180.985s</td>
      <td>17.0</td>
      <td>Three runs average</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik9] [1] (command line interface implementations) - revision [b69b99d] [2]:
<table>
  <thead>
    <tr>
      <th>Language</th>
      <th>Real time</th>
      <th>User time</th>
      <th>Sys time</th>
      <th>Execution environment</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>C</td>
      <td>71.553s</td>
      <td>70.545s</td>
      <td>0.791s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, SSE optimizations (vectors and prefetching)</td>
    </tr>
    <tr>
      <td>C</td>
      <td>115.379s</td>
      <td>114.378s</td>
      <td>0.800s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, no SSE optimizations</td>
    </tr>
    <tr>
      <td>Java</td>
      <td>197.634s</td>
      <td>196.535s</td>
      <td>1.204s</td>
      <td>Oracle JDK 7 update 9</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>1186.708s</td>
      <td>1177.413s</td>
      <td>7.060s</td>
      <td>PyPy 1.9.0</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>16858.427s</td>
      <td>16850.569s</td>
      <td>2.932s</td>
      <td>CPython 2.7.3</td>
      <td>Single run, without -OO</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik9] [1] (JavaScript in browsers) - revision [b69b99d] [2]:
<table>
  <thead>
    <tr>
      <th>Browser</th>
      <th>Reported time</th>
      <th>Browser version</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Chrome</td>
      <td>456.905s</td>
      <td>23.0.1271.95</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Opera</td>
      <td>1305.686s</td>
      <td>12.11 (build 1661)</td>
      <td>Three runs average</td>
    </tr>
  </tbody>
</table>

I have failed to get working the following execution environments:

 - ShedSkin (Python)
 - GNU GCJ (Java)
 - Jython (Python)



  [1]: http://mattmahoney.net/dc/textdata.html "LTCB: About the Test data"
  [2]: https://github.com/tarsa/TarsaLZP/commit/b69b99d775981668b8b09a71f181db3959955640 "b69b99d775981668b8b09a71f181db3959955640"

