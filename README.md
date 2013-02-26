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

Encoding speed on [enwik8] [1] (command line interface implementations) - revision [53a85ed] [2]:
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
      <td>7.473s</td>
      <td>7.165s</td>
      <td>0.276s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, SSE optimizations (vectors and prefetching)</td>
    </tr>
    <tr>
      <td>C</td>
      <td>12.734s</td>
      <td>12.437s</td>
      <td>0.279s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, no SSE optimizations</td>
    </tr>
    <tr>
      <td>Java</td>
      <td>20.587s</td>
      <td>20.326s</td>
      <td>0.280s</td>
      <td>Oracle JDK 7 update 15</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>60.346s</td>
      <td>59.907s</td>
      <td>0.367s</td>
      <td>ShedSkin 0.9.3 + GNU GCC 4.6.3</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>139.983s</td>
      <td>132.384s</td>
      <td>6.339s</td>
      <td>PyPy 1.9.0</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>1806.230s</td>
      <td>1804.573s</td>
      <td>0.436s</td>
      <td>CPython 2.7.3</td>
      <td>Single run, -OO</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik8] [1] (JavaScript in browsers) - revision [53a85ed] [2]:
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
      <td>47.136s</td>
      <td>25.0.1364.97</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Opera</td>
      <td>137.397s</td>
      <td>12.11 (build 1661)</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Firefox</td>
      <td>153.289s</td>
      <td>19.0</td>
      <td>Three runs average</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik9] [1] (command line interface implementations) - revision [53a85ed] [2]:
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
      <td>67.853s</td>
      <td>66.129s</td>
      <td>0.764s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, SSE optimizations (vectors and prefetching)</td>
    </tr>
    <tr>
      <td>C</td>
      <td>114.255s</td>
      <td>112.308s</td>
      <td>0.757s</td>
      <td>GNU GCC 4.6.3</td>
      <td>Three runs average, no SSE optimizations</td>
    </tr>
    <tr>
      <td>Java</td>
      <td>189.163s</td>
      <td>186.413s</td>
      <td>0.845s</td>
      <td>Oracle JDK 7 update 15</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>545.170s</td>
      <td>540.163s</td>
      <td>1.023s</td>
      <td>ShedSkin 0.9.3 + GNU GCC 4.6.3</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>1086.181s</td>
      <td>1069.226s</td>
      <td>6.840s</td>
      <td>PyPy 1.9.0</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Python</td>
      <td>16799.145s</td>
      <td>16789.525s</td>
      <td>3.668s</td>
      <td>CPython 2.7.3</td>
      <td>Single run, -OO</td>
    </tr>
  </tbody>
</table>

Encoding speed on [enwik9] [1] (JavaScript in browsers) - revision [53a85ed] [2]:
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
      <td>422.509s</td>
      <td>25.0.1364.97</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Opera</td>
      <td>1287.090s</td>
      <td>12.11 (build 1661)</td>
      <td>Three runs average</td>
    </tr>
    <tr>
      <td>Firefox</td>
      <td>1434.559s</td>
      <td>19.0</td>
      <td>Three runs average</td>
    </tr>
  </tbody>
</table>



  [1]: http://mattmahoney.net/dc/textdata.html "LTCB: About the Test data"
  [2]: https://github.com/tarsa/TarsaLZP/commit/53a85edea75516883076f4a5d0c32e98ae7e8aaa "53a85edea75516883076f4a5d0c32e98ae7e8aaa"

